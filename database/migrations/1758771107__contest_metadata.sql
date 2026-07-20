-- Creation Date: 2025-09-25 03:31:47.967893+00:00 UTC
-- Reason: contest metadata

CREATE POLICY "Judges can view verified bonsai in their contests." ON kodama.bonsai
FOR SELECT TO authenticated
USING (
    kodama.is_bonsai_verified(id)
    AND
    EXISTS (
        SELECT 1
        FROM kodama.contest_participants
        WHERE contest_participants.contest_id = bonsai.contest_id
          AND contest_participants.user_id = auth.uid()
          AND contest_participants.role IN ('judge', 'head_judge')
          AND (
              contest_participants.role = 'head_judge'
              OR
              contest_participants.contest_class_id = bonsai.contest_class_id
          )
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
