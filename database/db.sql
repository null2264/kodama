DROP SCHEMA IF EXISTS kodama CASCADE;  -- TODO: Remove this after prod is deployed

CREATE SCHEMA kodama;

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
    'accepting',
    'reviewing_phase_1',
    'reviewing_phase_2',
    'finished'
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

CREATE TABLE kodama.profiles (
    id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username text UNIQUE,
    role kodama.role NOT NULL DEFAULT 'user'
);

ALTER TABLE kodama.profiles ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION kodama.is_admin()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    RETURN (
        SELECT role = 'admin' FROM kodama.profiles WHERE id = auth.uid()
    );
END;
$$;


CREATE TABLE kodama.bonsai_classes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL UNIQUE, -- "Shohin", "Chuhin", etc. must be unique
    description text
    -- TODO: Maybe later?
    -- created_at timestamptz NOT NULL DEFAULT now(),
    -- updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE kodama.bonsai_classes ENABLE ROW LEVEL SECURITY;

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
    state kodama.contest_state NOT NULL
);

ALTER TABLE kodama.contests ENABLE ROW LEVEL SECURITY;

-- Admins can do anything on contests.
CREATE POLICY "Admins can manage contests." ON kodama.contests
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Authenticated users can see contests that are not in 'draft' state.
CREATE POLICY "Authenticated users can view active contests." ON kodama.contests
FOR SELECT TO authenticated
USING (state <> 'draft');

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
    owner_id uuid NOT NULL REFERENCES auth.users(id),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    contest_class_id uuid NOT NULL REFERENCES kodama.contest_classes(id) ON DELETE RESTRICT,
    state kodama.bonsai_state NOT NULL DEFAULT 'draft',
    is_phase_2_candidate boolean NOT NULL DEFAULT false
);

ALTER TABLE kodama.bonsai ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can create their own bonsai." ON kodama.bonsai
FOR INSERT TO authenticated
WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "Users can view their own bonsai." ON kodama.bonsai
FOR SELECT
USING (auth.uid() = owner_id);

CREATE POLICY "Users can update their own bonsai." ON kodama.bonsai
FOR UPDATE TO authenticated
USING (auth.uid() = owner_id);

CREATE POLICY "Users can delete their own DRAFT bonsai." ON kodama.bonsai
FOR DELETE TO authenticated
USING (auth.uid() = owner_id AND state = 'draft');

CREATE TABLE kodama.contest_participants (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES auth.users(id),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    role kodama.contest_role NOT NULL,

    -- A judge is assigned to a specific class *within a contest*.
    -- This points to the junction table record. It's nullable for Head Judges/Contestants.
    contest_class_id uuid NULL REFERENCES kodama.contest_classes(id) ON DELETE SET NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(user_id, contest_id, role, contest_class_id),

    CONSTRAINT role_requires_class_id
    CHECK (
        (role <> 'judge') OR (contest_class_id IS NOT NULL)
    )
);

ALTER TABLE kodama.contest_participants ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Judges can view verified bonsai in their contests." ON kodama.bonsai
FOR SELECT TO authenticated
USING (
    state = 'verified' AND
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
BEGIN
    -- This function is called after an update on the bonsai table.
    -- NEW refers to the row's data *after* the update.
    -- OLD refers to the row's data *before* the update.

    -- We only care about the moment it becomes 'verified'.
    -- We also check OLD.state to ensure this only fires once per bonsai.
    IF NEW.state = 'verified' AND OLD.state <> 'verified' THEN

        -- Insert the owner as a 'contestant' for that contest.
        -- This runs with the permissions of the user who triggered it (the admin).
        INSERT INTO kodama.contest_participants (user_id, contest_id, role)
        VALUES (NEW.owner_id, NEW.contest_id, 'contestant')
        -- If the user already submitted another bonsai and is already a contestant,
        -- this clause gracefully does nothing instead of causing an error.
        ON CONFLICT (user_id, contest_id, role) DO NOTHING;

    END IF;

    -- The trigger function must return NEW for an AFTER trigger.
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_bonsai_verified ON kodama.bonsai; -- TODO: Remove once prod is deployed

-- Create the new trigger
CREATE TRIGGER on_bonsai_verified
-- It should run AFTER the update is successfully committed to the table.
AFTER UPDATE ON kodama.bonsai
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
    -- What phase is this review for?
    phase kodama.review_phase NOT NULL,
    -- The score given by the judge
    score integer NOT NULL CHECK (score >= 0 AND score <= 100), -- Example score range
    comments text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- A judge can only review a specific bonsai once per phase.
    UNIQUE(bonsai_id, judge_id, phase)
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

ALTER TABLE kodama.reviews ENABLE ROW LEVEL SECURITY;

-- POLICY 1: Allow judges to submit reviews during Phase 1
CREATE POLICY "Judges can submit reviews in Phase 1 for their assigned class" ON kodama.reviews
FOR INSERT TO authenticated
WITH CHECK (
    -- The review being submitted must be for phase_1
    phase = 'phase_1'
    AND
    -- The user must be a designated judge or head judge for the contest
    (
        SELECT EXISTS (
            SELECT 1
            FROM kodama.bonsai b
            JOIN kodama.contest_participants p ON b.contest_id = p.contest_id
            WHERE
                b.id = reviews.bonsai_id
                AND p.user_id = auth.uid()
                AND b.state = 'verified' -- Can only review verified bonsai
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

-- POLICY 2: Allow judges to submit reviews during Phase 2
CREATE POLICY "Judges can submit reviews in Phase 2 for candidate bonsai" ON kodama.reviews
FOR INSERT TO authenticated
WITH CHECK (
    -- The review being submitted must be for phase_2
    phase = 'phase_2'
    AND
    -- Check that the bonsai is a phase 2 candidate and the user is a judge for the contest
    (
        SELECT EXISTS (
            SELECT 1
            FROM kodama.bonsai b
            WHERE b.id = reviews.bonsai_id
            AND b.is_phase_2_candidate = true -- The bonsai MUST be a P2 candidate
            AND kodama.is_contest_judge(b.contest_id) -- Any judge for the contest can review in P2
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
  UPDATE kodama.bonsai
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
--#endregion
