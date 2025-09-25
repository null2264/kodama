-- Creation Date: 2025-09-25 04:29:48.452072+00:00 UTC
-- Reason: contest review nightmare

CREATE TABLE kodama.reviews (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    bonsai_id uuid NOT NULL REFERENCES kodama.bonsai(id) ON DELETE CASCADE,
    judge_id uuid NOT NULL REFERENCES auth.users(id),

    -- The structured scores for Phase 1
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

CREATE OR REPLACE FUNCTION kodama.validate_phase1_score()
RETURNS TRIGGER AS $$
DECLARE
  v_required_keys text[] := array['penampilan', 'gerak_dasar', 'keserasian', 'kematangan'];
  v_submitted_keys text[];
BEGIN
  -- Check for correct keys
  v_submitted_keys := array(SELECT jsonb_object_keys(NEW.scores));
  IF NOT (v_submitted_keys @> v_required_keys AND v_submitted_keys <@ v_required_keys) THEN
    RAISE EXCEPTION 'Submitted scores have mismatched criteria. Expected: %, Got: %', v_required_keys, v_submitted_keys;
  END IF;

  -- Calculate total score
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
EXECUTE FUNCTION kodama.validate_phase1_score();

ALTER TABLE kodama.reviews ENABLE ROW LEVEL SECURITY;

-- POLICY 1: Allow judges to submit reviews during Phase 1
CREATE POLICY "Judges can submit reviews in Phase 1 for their assigned class" ON kodama.reviews
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
                AND c.state = 'reviewing_phase_1'
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
                    -- Head Judges can review any class in Phase 1
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

CREATE OR REPLACE FUNCTION kodama.advance_to_phase_2(p_contest_id uuid)
RETURNS void -- It doesn't need to return anything
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_is_privileged boolean;
BEGIN
  -- 1. Security Check: Ensure the user is an Admin or the Head Judge for this contest.
  SELECT (
    kodama.is_admin() OR
    EXISTS (
        SELECT 1 FROM kodama.contest_participants
        WHERE user_id = auth.uid()
          AND contest_id = p_contest_id
          AND role = 'head_judge'
    )
  ) INTO v_is_privileged;

  IF NOT v_is_privileged THEN
    RAISE EXCEPTION 'User does not have permission to advance this contest.';
  END IF;

  -- 2. Logic: Find the top 10 bonsai in each class based on Phase 1 scores
  --    and mark them as phase 2 candidates.
  WITH ranked_bonsai AS (
    SELECT
      b.id as bonsai_id,
      -- Rank bonsai within each class based on the average score
      ROW_NUMBER() OVER(
        PARTITION BY b.contest_class_id
        ORDER BY AVG(r.score) DESC, b.created_at ASC -- Tie-break with submission time
      ) as rank
    FROM kodama.bonsai b
    JOIN kodama.reviews r ON b.id = r.bonsai_id
    WHERE b.contest_id = p_contest_id AND r.phase = 'phase_1'
    GROUP BY b.id, b.contest_class_id
  )
  UPDATE kodama.bonsai_metadata
  SET is_phase_2_candidate = true
  WHERE id IN (
    SELECT bonsai_id FROM ranked_bonsai WHERE rank <= 10
  );

  -- 3. State Change: Update the contest's overall state to Phase 2.
  UPDATE kodama.contests
  SET state = 'reviewing_phase_2'
  WHERE id = p_contest_id;

END;
$$;

CREATE TABLE kodama.phase_2_votes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    judge_id uuid NOT NULL REFERENCES auth.users(id),
    -- The specific bonsai the judge voted for.
    bonsai_id uuid NOT NULL REFERENCES kodama.bonsai(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),

    -- THE MOST IMPORTANT RULE: A judge can only vote ONCE per contest.
    UNIQUE(contest_id, judge_id)
);

ALTER TABLE kodama.phase_2_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Judges can cast their Best in Show vote." ON kodama.phase_2_votes
FOR INSERT TO authenticated
WITH CHECK (
  -- The user must be a judge for this contest.
  kodama.is_contest_judge(contest_id)
  AND
  -- The contest must be in the correct phase.
  (SELECT state FROM kodama.contests WHERE id = contest_id) = 'reviewing_phase_2'
  AND
  -- The bonsai being voted for MUST be a Phase 2 candidate.
  (SELECT is_phase_2_candidate FROM kodama.bonsai_metadata WHERE id = bonsai_id) = true
);

-- Policy for viewing votes. To prevent influencing other judges,
-- let's say only Admins and Head Judges can see the vote counts.
CREATE POLICY "Admins and Head Judges can see all votes." ON kodama.phase_2_votes
FOR SELECT TO authenticated
USING (
    kodama.is_admin() OR
    EXISTS (
        SELECT 1 FROM kodama.contest_participants p
        WHERE p.contest_id = phase_2_votes.contest_id
          AND p.user_id = auth.uid()
          AND p.role = 'head_judge'
    )
);

-- Optional: Allow a judge to see their own vote after casting it.
CREATE POLICY "Judges can see their own vote." ON kodama.phase_2_votes
FOR SELECT TO authenticated
USING (judge_id = auth.uid());
