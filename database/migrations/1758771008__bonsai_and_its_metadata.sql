-- Creation Date: 2025-09-25 03:30:08.830972+00:00 UTC
-- Reason: bonsai and its metadata

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

--#region Helper functions
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
--#endregion

CREATE POLICY "Users can view their own bonsai metadata." ON kodama.bonsai_metadata
FOR SELECT
USING (kodama.is_bonsai_owner(id, auth.uid()));

CREATE POLICY "Users can delete their own DRAFT bonsai." ON kodama.bonsai
FOR DELETE TO authenticated
USING (auth.uid() = owner_id AND kodama.is_bonsai_in_draft(id));

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
