-- NOTE: To self -> UPSERT requires policy for SELECT, UPDATE, and INSERT

DROP SCHEMA IF EXISTS kodama CASCADE;  -- TODO: Remove this after prod is deployed

CREATE SCHEMA kodama;

--#region Grants bs so supabase/postgrest can stop crying
-- We need to do this otherwise supabase (or rather postgrest) would cry about not having permission
-- REF: https://github.com/supabase/supabase/blob/a2fc6d592cb4ea50fd518b99db199a31912040b9/docker/volumes/db/init/00-initial-schema.sql#L26-L29
grant usage              on schema kodama to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on tables to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on functions to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on sequences to postgres, anon, authenticated, service_role;
--#endregion

--#region Enums / Custom Types
CREATE TYPE kodama.role AS ENUM (
    'user',
    'admin'
);

CREATE TYPE kodama.contest_role AS ENUM (
    'contestant',
    -- TODO:
    -- Judges are bound to their respective bonsai classes
    'judge',
    -- TODO:
    -- Head Judge has extra privilege to accept or deny Judge's request for things like revising a review.
    'head_judge'
);

-- TODO: Handle 2 phase review
CREATE TYPE kodama.bonsai_state AS ENUM (
    'draft',
    -- FIXME: Maybe this state is not needed? since if the state turn to 'waiting_verify' then the bonsai is already "finalized"
    --'waiting_payment',
    -- NOTE: Admin need to verify the payment manually
    'waiting_verify',
    'verified'
);

CREATE TYPE kodama.contest_state AS ENUM (
    'draft',
    'accepting',  -- Draft is finalized and is now accepting registration
    'closed',  -- Registration is closed
    'reviewing_phase_1',  -- Phase 1 start, scoring in progress, scores are locked upon being submitted but can still be edited if head judge permits it
    'discuss_phase_2',  -- Phase 1 end, scores are fully locked, in this state judges (and head judge) can discuss about which bonsai to be crowned as "Best in Show"
    'reviewing_phase_2',  -- Phase 2 start, voting start, judges are now able to vote which bonsai to be crowned as "Best in Show"
    'finished'  -- Phase 2 end, contest is over, show the result
);

CREATE TYPE kodama.review_phase AS ENUM ('phase_1', 'phase_2');
--#endregion

CREATE OR REPLACE FUNCTION kodama.set_current_timestamp_updated_at()
RETURNS TRIGGER
SET search_path = ''
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE kodama.user_metadata (
    id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    role kodama.role NOT NULL DEFAULT 'user'
);

-- Insert existing users' metadata
INSERT INTO kodama.user_metadata (id, role)
SELECT id, 'user'
FROM auth.users;

ALTER TABLE kodama.user_metadata ENABLE ROW LEVEL SECURITY;

-- Create new metadata when there's a new user
CREATE OR REPLACE FUNCTION kodama.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  INSERT INTO kodama.user_metadata (id, role)
  VALUES (new.id, 'user');
  RETURN new;
END;
$$;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE PROCEDURE kodama.handle_new_user();

CREATE OR REPLACE FUNCTION kodama.is_admin()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT role = 'admin' FROM kodama.user_metadata WHERE id = auth.uid()
    );
END;
$$;

-- TODO: Already handled by supabase's dashboard but maybe we can add "super admin" later that can promote normal user to admin
-- CREATE POLICY "Admin can update a user's metadata." ON kodama.user_metadata
-- FOR UPDATE TO authenticated
-- USING (kodama.is_admin());
-- CREATE POLICY "Admin can see a user's metadata." ON kodama.user_metadata
-- FOR SELECT TO authenticated
-- USING (kodama.is_admin());


CREATE TABLE kodama.bonsai_classes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL UNIQUE, -- "Shohin", "Chuhin", etc. must be unique
    description text
    -- TODO: Maybe later?
    -- created_at timestamptz NOT NULL DEFAULT now(),
    -- updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE kodama.bonsai_classes ENABLE ROW LEVEL SECURITY;

INSERT INTO kodama.bonsai_classes (id, name)
VALUES
('8f8e46fd-75e9-443e-a1f2-7ffab68ece31', 'Prospek'),
('fb85ea10-0fc5-40b9-9e27-f236962c8271', 'Pratama'),
('11943061-aa9d-4cc0-90f6-2ca7b70bc1b5', 'Madya');

-- RLS: Only admins should manage the master list of classes.
CREATE POLICY "Admins can manage master bonsai classes." ON kodama.bonsai_classes
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Everyone can read the class definitions.
CREATE POLICY "All users can view master bonsai classes." ON kodama.bonsai_classes
FOR SELECT USING (true);


CREATE TABLE kodama.contests (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL,
    description text,
    state kodama.contest_state NOT NULL DEFAULT 'draft'
);

ALTER TABLE kodama.contests ENABLE ROW LEVEL SECURITY;

-- Admins can do anything on contests.
CREATE POLICY "Admins can manage contests." ON kodama.contests
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Authenticated users can see contests that are not in 'draft' state.
CREATE POLICY "Authenticated users can view active contests." ON kodama.contests
FOR SELECT TO authenticated
USING (state <> 'draft');

CREATE OR REPLACE FUNCTION kodama.is_registration_open(contest_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT state = 'accepting' FROM kodama.contests WHERE id = contest_id
    );
END;
$$;

-- This helps us archive contest classes in case a class is added/removed in the future without affecting old contests.
CREATE TABLE kodama.contest_classes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    class_id uuid NOT NULL REFERENCES kodama.bonsai_classes(id) ON DELETE RESTRICT,
    UNIQUE(contest_id, class_id)
);

ALTER TABLE kodama.contest_classes ENABLE ROW LEVEL SECURITY;

-- RLS Policies for the junction table
-- Admins can link classes to contests.
CREATE POLICY "Admins can manage contest class offerings." ON kodama.contest_classes
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Users can see which classes are offered in an active contest.
CREATE POLICY "Users can view classes for active contests." ON kodama.contest_classes
FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM kodama.contests c
        WHERE c.id = contest_classes.contest_id AND c.state <> 'draft'
    )
);


CREATE TABLE kodama.bonsai (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL,
    owner_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    contest_class_id uuid NOT NULL REFERENCES kodama.contest_classes(id) ON DELETE RESTRICT
);

ALTER TABLE kodama.bonsai ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can create their own bonsai." ON kodama.bonsai
FOR INSERT TO authenticated
WITH CHECK (auth.uid() = owner_id AND kodama.is_registration_open(contest_id));

CREATE POLICY "Users can view their own bonsai." ON kodama.bonsai
FOR SELECT
USING (auth.uid() = owner_id);

CREATE POLICY "Users can update their own bonsai." ON kodama.bonsai
FOR UPDATE TO authenticated
USING (auth.uid() = owner_id AND kodama.is_registration_open(contest_id));

-- Metadata for kodama.bonsai table for columns that should only be able to be modified by the user in a special way, like RPC or SQL Function
CREATE TABLE kodama.bonsai_metadata (
    id uuid PRIMARY KEY REFERENCES kodama.bonsai(id) ON DELETE CASCADE,
    state kodama.bonsai_state NOT NULL DEFAULT 'draft',
    is_phase_2_candidate boolean NOT NULL DEFAULT false
);

ALTER TABLE kodama.bonsai_metadata ENABLE ROW LEVEL SECURITY;

-- Create new metadata when there's a new bonsai
CREATE OR REPLACE FUNCTION kodama.handle_new_bonsai()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
  INSERT INTO kodama.bonsai_metadata (id)
  VALUES (new.id);
  RETURN new;
END;
$$;
CREATE TRIGGER on_bonsai_created
AFTER INSERT ON kodama.bonsai
FOR EACH ROW EXECUTE PROCEDURE kodama.handle_new_bonsai();

CREATE OR REPLACE FUNCTION kodama.is_bonsai_in_draft(bonsai_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT state = 'draft' FROM kodama.bonsai_metadata WHERE id = is_bonsai_in_draft.bonsai_id
    );
END;
$$;

CREATE OR REPLACE FUNCTION kodama.is_bonsai_owner(bonsai_id uuid, owner_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT bonsai.owner_id = is_bonsai_owner.owner_id FROM kodama.bonsai WHERE id = is_bonsai_owner.bonsai_id
    );
END;
$$;

CREATE OR REPLACE FUNCTION kodama.finalize_bonsai(bonsai_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    IF NOT kodama.is_bonsai_owner(bonsai_id, auth.uid()) THEN
        RAISE EXCEPTION 'User does not own this bonsai.';
    END IF;

    UPDATE kodama.bonsai_metadata
    SET state = 'waiting_verify'
    WHERE id = finalize_bonsai.bonsai_id;

    RETURN (
        SELECT state = 'waiting_verify' FROM kodama.bonsai_metadata WHERE id = finalize_bonsai.bonsai_id
    );
END;
$$;

CREATE OR REPLACE FUNCTION kodama.verify_bonsai(bonsai_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    IF NOT kodama.is_admin() THEN
        RAISE EXCEPTION 'User is not an admin.';
    END IF;

    UPDATE kodama.bonsai_metadata
    SET state = 'verified'
    WHERE id = verify_bonsai.bonsai_id;

    RETURN (
        SELECT state = 'verified' FROM kodama.bonsai_metadata WHERE id = verify_bonsai.bonsai_id
    );
END;
$$;

CREATE POLICY "Users can view their own bonsai metadata." ON kodama.bonsai_metadata
FOR SELECT
USING (kodama.is_bonsai_owner(id, auth.uid()));


CREATE POLICY "Users can delete their own DRAFT bonsai." ON kodama.bonsai
FOR DELETE TO authenticated
USING (auth.uid() = owner_id AND kodama.is_bonsai_in_draft(id));

CREATE TABLE kodama.contest_participants (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES auth.users(id),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    role kodama.contest_role NOT NULL,

    -- A judge is assigned to a specific class *within a contest*.
    -- This points to the junction table record. It's nullable for Head Judges/Contestants.
    contest_class_id uuid NULL REFERENCES kodama.contest_classes(id) ON DELETE SET NULL,

    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT role_requires_class_id
    CHECK (
        (role <> 'judge') OR (contest_class_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX contest_participants_idx
ON kodama.contest_participants (user_id, contest_id, role, contest_class_id) NULLS NOT DISTINCT;

ALTER TABLE kodama.contest_participants ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Judges can view verified bonsai in their contests." ON kodama.bonsai
FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1
        FROM kodama.bonsai_metadata
        WHERE bonsai_metadata.id = bonsai.id
            AND bonsai_metadata.state = 'verified'
    )
    AND
    EXISTS (
        SELECT 1
        FROM kodama.contest_participants
        WHERE contest_participants.contest_id = bonsai.contest_id
          AND contest_participants.user_id = auth.uid()
          AND contest_participants.role IN ('judge', 'head_judge')
    )
);

CREATE OR REPLACE FUNCTION kodama.handle_bonsai_verification()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    bonsai_owner_id uuid;
    bonsai_contest_id uuid;
BEGIN
    -- This function is called after an update on the bonsai table.
    -- NEW refers to the row's data *after* the update.
    -- OLD refers to the row's data *before* the update.

    -- We only care about the moment it becomes 'verified'.
    -- We also check OLD.state to ensure this only fires once per bonsai.
    IF NEW.state = 'verified' AND OLD.state <> 'verified' THEN
        SELECT owner_id, contest_id INTO bonsai_owner_id, bonsai_contest_id FROM kodama.bonsai WHERE id = NEW.id;

        -- Insert the owner as a 'contestant' for that contest.
        -- This runs with the permissions of the user who triggered it (the admin).
        INSERT INTO kodama.contest_participants (user_id, contest_id, role, contest_class_id)
        VALUES (bonsai_owner_id, bonsai_contest_id, 'contestant', NULL)  -- contest_class_id is always NULL for contestant
        -- If the user already submitted another bonsai and is already a contestant,
        -- this clause gracefully does nothing instead of causing an error.
        ON CONFLICT (user_id, contest_id, role, contest_class_id) DO NOTHING;

    END IF;

    -- The trigger function must return NEW for an AFTER trigger.
    RETURN NEW;
END;
$$;

-- Create the new trigger
CREATE TRIGGER on_bonsai_verified
-- It should run AFTER the update is successfully committed to the table.
AFTER UPDATE ON kodama.bonsai_metadata
FOR EACH ROW
-- This is the condition for when the trigger function should even be called.
-- It's a performance optimization.
WHEN (OLD.state IS DISTINCT FROM NEW.state)
-- This is the function to execute.
EXECUTE FUNCTION kodama.handle_bonsai_verification();


--#region Review stuff
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
--#endregion
