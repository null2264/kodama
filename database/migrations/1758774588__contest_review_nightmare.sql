-- Creation Date: 2025-09-25 04:29:48.452072+00:00 UTC
-- Reason: contest review nightmare

CREATE TABLE kodama.reviews (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    bonsai_id uuid NOT NULL REFERENCES kodama.bonsai(id) ON DELETE CASCADE,
    judge_id uuid NOT NULL REFERENCES auth.users(id),

    -- {"penampilan": 100, "gerak_dasar": 100, "keserasian": 100, "kematangan": 100}
    scores jsonb NOT NULL,
    total_score integer NOT NULL,

    comments text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- A judge can only review a bonsai once.
    UNIQUE(bonsai_id, judge_id)
);

-- Add the updated_at trigger
CREATE TRIGGER set_reviews_updated_at
BEFORE UPDATE ON kodama.reviews
FOR EACH ROW
EXECUTE FUNCTION kodama.set_current_timestamp_updated_at();

-- Let's define a helper function to check if the current user is a judge/head-judge for a contest
CREATE OR REPLACE FUNCTION kodama.is_contest_judge(p_contest_id uuid)
RETURNS boolean AS $$
  SELECT EXISTS (
    SELECT 1 FROM kodama.contest_participants
    WHERE contest_id = p_contest_id
      AND user_id = auth.uid()
      AND role IN ('judge', 'head_judge')
  );
$$ LANGUAGE sql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION kodama.validate_score()
RETURNS TRIGGER AS $$
DECLARE
  v_required_keys text[] := array['penampilan', 'gerak_dasar', 'keserasian', 'kematangan'];
  v_submitted_keys text[];
BEGIN
  IF TG_OP = 'UPDATE' THEN
    RAISE EXCEPTION 'Scores cannot be edited after submission.';
  END IF;

  v_submitted_keys := array(SELECT jsonb_object_keys(NEW.scores));
  IF NOT (v_submitted_keys @> v_required_keys AND v_submitted_keys <@ v_required_keys) THEN
    RAISE EXCEPTION 'Submitted scores have mismatched criteria. Expected: %, Got: %', v_required_keys, v_submitted_keys;
  END IF;

  NEW.total_score := (
    SELECT sum(value::numeric)
    FROM jsonb_each_text(NEW.scores)
  );

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_review_submit
BEFORE INSERT OR UPDATE ON kodama.reviews
FOR EACH ROW
EXECUTE FUNCTION kodama.validate_score();

ALTER TABLE kodama.reviews ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Judges can submit reviews for their assigned class" ON kodama.reviews
FOR INSERT TO authenticated
WITH CHECK (
    (
        -- The user must be a designated judge or head judge for the contest
        SELECT EXISTS (
            SELECT 1
            FROM kodama.contests c
            JOIN kodama.bonsai b ON c.id = b.contest_id
            WHERE
                b.id = reviews.bonsai_id
                AND c.state = 'reviewing'
        )
    )
    AND
    (
        -- The user must be a designated judge or head judge for the contest
        SELECT EXISTS (
            SELECT 1
            FROM kodama.bonsai b
            JOIN kodama.contest_participants p ON b.contest_id = p.contest_id
            JOIN kodama.bonsai_metadata m ON b.id = m.id
            WHERE
                b.id = reviews.bonsai_id
                AND p.user_id = auth.uid()
                AND m.state = 'verified' -- Can only review verified bonsai
                AND (
                    p.role = 'head_judge'
                    OR
                    -- Regular judges are restricted to their assigned class
                    (p.role = 'judge' AND p.contest_class_id = b.contest_class_id)
                )
        )
    )
);

-- POLICY 3: Users can see their own reviews.
CREATE POLICY "Judges can see their own reviews." ON kodama.reviews
FOR SELECT TO authenticated
USING (judge_id = auth.uid());

-- POLICY 4: Admins/Head Judges can see all reviews.
CREATE POLICY "Admins and Head Judges can see all reviews for a contest." ON kodama.reviews
FOR SELECT TO authenticated
USING (
    kodama.is_admin() OR
    (
        SELECT EXISTS (
            SELECT 1
            FROM kodama.bonsai b
            JOIN kodama.contest_participants p ON b.contest_id = p.contest_id
            WHERE b.id = reviews.bonsai_id
              AND p.user_id = auth.uid()
              AND p.role = 'head_judge'
        )
    )
);

CREATE OR REPLACE FUNCTION kodama.best_in_show(p_contest_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
DECLARE
  v_winner_id uuid;
  v_total_bonsai integer;
BEGIN
  IF NOT (kodama.is_admin() OR EXISTS (
    SELECT 1 FROM kodama.contest_participants
    WHERE user_id = auth.uid()
      AND contest_id = p_contest_id
      AND role = 'head_judge'
  )) THEN
    RAISE EXCEPTION 'User does not have permission to determine best in show.';
  END IF;

  IF (SELECT state FROM kodama.contests WHERE id = p_contest_id) <> 'reviewing' THEN
    RAISE EXCEPTION 'Contest is not in reviewing phase.';
  END IF;

  SELECT COUNT(DISTINCT r.bonsai_id) INTO v_total_bonsai
  FROM kodama.bonsai b
  JOIN kodama.reviews r ON b.id = r.bonsai_id
  WHERE b.contest_id = p_contest_id;

  IF v_total_bonsai = 0 THEN
    RAISE EXCEPTION 'No reviewed bonsai found in this contest.';
  END IF;

  SELECT b.id INTO v_winner_id
  FROM kodama.bonsai b
  JOIN kodama.reviews r ON b.id = r.bonsai_id
  WHERE b.contest_id = p_contest_id
  GROUP BY b.id, b.created_at
  ORDER BY AVG(r.total_score) DESC, b.created_at ASC
  LIMIT 1;

  UPDATE kodama.contests
  SET state = 'finished'
  WHERE id = p_contest_id;

  RETURN v_winner_id;
END;
$$;

CREATE OR REPLACE FUNCTION kodama.finish_contest(p_contest_id uuid, p_force boolean DEFAULT false)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
DECLARE
  v_total_verified integer;
  v_total_reviewed integer;
  v_winner_id uuid;
BEGIN
  IF NOT (kodama.is_admin() OR EXISTS (
    SELECT 1 FROM kodama.contest_participants
    WHERE user_id = auth.uid()
      AND contest_id = p_contest_id
      AND role = 'head_judge'
  )) THEN
    RAISE EXCEPTION 'User does not have permission to finish this contest.';
  END IF;

  IF (SELECT state FROM kodama.contests WHERE id = p_contest_id) <> 'reviewing' THEN
    RAISE EXCEPTION 'Contest is not in reviewing phase.';
  END IF;

  SELECT COUNT(*) INTO v_total_verified
  FROM kodama.bonsai b
  JOIN kodama.bonsai_metadata m ON b.id = m.id
  WHERE b.contest_id = p_contest_id AND m.state = 'verified';

  SELECT COUNT(DISTINCT r.bonsai_id) INTO v_total_reviewed
  FROM kodama.bonsai b
  JOIN kodama.reviews r ON b.id = r.bonsai_id
  WHERE b.contest_id = p_contest_id;

  IF NOT p_force AND v_total_reviewed < v_total_verified THEN
    RAISE EXCEPTION 'Not all verified bonsai have been reviewed (%).', v_total_reviewed;
  END IF;

  SELECT b.id INTO v_winner_id
  FROM kodama.bonsai b
  JOIN kodama.reviews r ON b.id = r.bonsai_id
  WHERE b.contest_id = p_contest_id
  GROUP BY b.id, b.created_at
  ORDER BY AVG(r.total_score) DESC, b.created_at ASC
  LIMIT 1;

  UPDATE kodama.contests
  SET state = 'finished'
  WHERE id = p_contest_id;

  RETURN v_winner_id;
END;
$$;

-- ================================================================
-- State machine enforcement
-- ================================================================

CREATE OR REPLACE FUNCTION kodama.validate_contest_state_transition()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.state = 'draft' AND NEW.state = 'accepting' THEN RETURN NEW; END IF;
  IF OLD.state = 'accepting' AND NEW.state = 'closed' THEN RETURN NEW; END IF;
  IF OLD.state = 'closed' AND NEW.state = 'reviewing' THEN RETURN NEW; END IF;
  IF OLD.state = 'reviewing' AND NEW.state = 'finished' THEN RETURN NEW; END IF;
  IF OLD.state = 'finished' AND NEW.state = 'ended' THEN
    IF kodama.is_admin() THEN RETURN NEW; END IF;
    RAISE EXCEPTION 'Only admins can end a contest.';
  END IF;
  RAISE EXCEPTION 'Invalid contest state transition: % -> %', OLD.state, NEW.state;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_contest_state_change
BEFORE UPDATE OF state ON kodama.contests
FOR EACH ROW
WHEN (OLD.state IS DISTINCT FROM NEW.state)
EXECUTE FUNCTION kodama.validate_contest_state_transition();

-- ================================================================
-- Helper: check if a contest is still editable
-- ================================================================

CREATE OR REPLACE FUNCTION kodama.is_contest_editable(p_contest_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SET search_path = ''
AS $$
  SELECT state NOT IN ('finished', 'ended') FROM kodama.contests WHERE id = p_contest_id;
$$;

-- ================================================================
-- Helper: list users in a contest grouped by role
-- ================================================================

CREATE OR REPLACE FUNCTION kodama.get_contest_users(p_contest_id uuid)
RETURNS TABLE(user_id uuid, email text, role kodama.contest_role, contest_class_id uuid)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT p.user_id, u.email, p.role, p.contest_class_id
  FROM kodama.contest_participants p
  JOIN auth.users u ON u.id = p.user_id
  WHERE p.contest_id = p_contest_id
  ORDER BY p.role, u.email;
$$;
