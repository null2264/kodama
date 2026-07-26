-- Creation Date: 2025-09-25 03:30:08.830972+00:00 UTC
-- Reason: bonsai and its metadata

--#region Bonsai table

CREATE TABLE kodama.bonsai (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL,
    owner_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    contest_class_id uuid NOT NULL REFERENCES kodama.contest_classes(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    state kodama.bonsai_state NOT NULL DEFAULT 'draft',
    pict_path text,
    payment_proof_path text,
    UNIQUE(name, contest_id)
);

ALTER TABLE kodama.bonsai ENABLE ROW LEVEL SECURITY;

ALTER TABLE kodama.contests ADD COLUMN banner_path text;

--#endregion

--#region Helper functions

CREATE OR REPLACE FUNCTION kodama.is_bonsai_in_draft(bonsai_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT state = 'draft' FROM kodama.bonsai WHERE id = is_bonsai_in_draft.bonsai_id
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

CREATE OR REPLACE FUNCTION kodama.is_bonsai_verified(bonsai_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN COALESCE((SELECT state = 'verified' FROM kodama.bonsai WHERE id = is_bonsai_verified.bonsai_id), false);
END;
$$;

--#endregion

--#region RLS Policies

CREATE POLICY "Users can create their own bonsai." ON kodama.bonsai
FOR INSERT TO authenticated
WITH CHECK (auth.uid() = owner_id AND kodama.is_registration_open(contest_id));

CREATE POLICY "Users can view their own bonsai or all entries in finished contests." ON kodama.bonsai
FOR SELECT
USING (
    auth.uid() = owner_id
    OR
    EXISTS (
        SELECT 1 FROM kodama.contests
        WHERE id = bonsai.contest_id
          AND state IN ('finished', 'ended')
          AND EXISTS (
              SELECT 1 FROM kodama.contest_participants
              WHERE contest_id = bonsai.contest_id
                AND user_id = auth.uid()
                AND role = 'contestant'
          )
    )
);

CREATE POLICY "Users can update their own bonsai." ON kodama.bonsai
FOR UPDATE TO authenticated
USING (auth.uid() = owner_id AND kodama.is_registration_open(contest_id));

CREATE POLICY "Users can delete their own DRAFT bonsai." ON kodama.bonsai
FOR DELETE TO authenticated
USING (auth.uid() = owner_id AND kodama.is_bonsai_in_draft(id));

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

CREATE POLICY "Admins can view all bonsai." ON kodama.bonsai
FOR SELECT USING (kodama.is_admin());

--#endregion

--#region Triggers

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
    IF NEW.state = 'verified' AND OLD.state <> 'verified' THEN
        SELECT owner_id, contest_id INTO bonsai_owner_id, bonsai_contest_id FROM kodama.bonsai WHERE id = NEW.id;

        INSERT INTO kodama.contest_participants (user_id, contest_id, role, contest_class_id)
        VALUES (bonsai_owner_id, bonsai_contest_id, 'contestant', NULL)
        ON CONFLICT (user_id, contest_id, role, contest_class_id) DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER on_bonsai_verified
AFTER UPDATE ON kodama.bonsai
FOR EACH ROW
WHEN (OLD.state IS DISTINCT FROM NEW.state)
EXECUTE FUNCTION kodama.handle_bonsai_verification();

--#endregion

--#region Storage bucket

INSERT INTO storage.buckets (id, name, public)
VALUES ('kodama-images', 'kodama-images', true)
ON CONFLICT (id) DO NOTHING;

GRANT USAGE ON SCHEMA storage TO authenticated;
GRANT ALL ON storage.objects TO authenticated;

CREATE POLICY "Authenticated users can read images" ON storage.objects
FOR SELECT TO authenticated
USING (bucket_id = 'kodama-images');

CREATE POLICY "Owner can upload bonsai pict" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'kodama-images'
    AND kodama.is_bonsai_owner(
        (string_to_array(name, '/'))[2]::uuid,
        auth.uid()
    )
);

CREATE POLICY "Admin can upload contest banner" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'kodama-images'
    AND kodama.is_admin()
);

CREATE POLICY "Owner or admin can manage images" ON storage.objects
FOR ALL TO authenticated
USING (
    bucket_id = 'kodama-images'
    AND (
        kodama.is_admin()
        OR (
            EXISTS (
                SELECT 1 FROM kodama.bonsai
                WHERE id = (string_to_array(name, '/'))[2]::uuid
                  AND owner_id = auth.uid()
            )
        )
    )
);

--#endregion

--#region RPC: State transitions

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

    UPDATE kodama.bonsai
    SET state = 'waiting_verify'
    WHERE id = finalize_bonsai.bonsai_id;

    RETURN (
        SELECT state = 'waiting_verify' FROM kodama.bonsai WHERE id = finalize_bonsai.bonsai_id
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

    UPDATE kodama.bonsai
    SET state = 'verified'
    WHERE id = verify_bonsai.bonsai_id;

    RETURN (
        SELECT state = 'verified' FROM kodama.bonsai WHERE id = verify_bonsai.bonsai_id
    );
END;
$$;

--#endregion

--#region RPC: Queries

CREATE OR REPLACE FUNCTION kodama.get_bonsai_with_metadata(p_contest_id uuid)
RETURNS TABLE(
    id uuid,
    name text,
    owner_id uuid,
    contest_id uuid,
    contest_class_id uuid,
    created_at timestamptz,
    pict_path text,
    state kodama.bonsai_state,
    payment_proof_path text
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = 'kodama'
AS $$
  SELECT b.id, b.name, b.owner_id, b.contest_id, b.contest_class_id,
         b.created_at, b.pict_path, b.state, b.payment_proof_path
  FROM kodama.bonsai b
  WHERE b.contest_id = p_contest_id
  ORDER BY b.created_at;
$$;

--#endregion

--#region RPC: Image/banner paths

CREATE OR REPLACE FUNCTION kodama.get_bonsai_pict_path(bonsai_id uuid)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
DECLARE
    result text;
BEGIN
    SELECT pict_path INTO result FROM kodama.bonsai WHERE id = bonsai_id;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION kodama.set_bonsai_pict_path(bonsai_id uuid, path text)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    IF NOT kodama.is_bonsai_owner(bonsai_id, auth.uid()) THEN
        RAISE EXCEPTION 'User does not own this bonsai.';
    END IF;

    UPDATE kodama.bonsai
    SET pict_path = set_bonsai_pict_path.path
    WHERE id = set_bonsai_pict_path.bonsai_id;

    RETURN FOUND;
END;
$$;

CREATE OR REPLACE FUNCTION kodama.set_bonsai_payment_proof_path(p_bonsai_id uuid, p_path text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = 'kodama'
AS $$
BEGIN
    UPDATE kodama.bonsai
    SET payment_proof_path = p_path
    WHERE id = p_bonsai_id;
END;
$$;

CREATE OR REPLACE FUNCTION kodama.get_contest_banner_path(contest_id uuid)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
DECLARE
    result text;
BEGIN
    SELECT banner_path INTO result FROM kodama.contests WHERE id = contest_id;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION kodama.set_contest_banner_path(contest_id uuid, path text)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    IF NOT kodama.is_admin() THEN
        RAISE EXCEPTION 'User is not an admin.';
    END IF;

    UPDATE kodama.contests
    SET banner_path = set_contest_banner_path.banner_path
    WHERE id = set_contest_banner_path.contest_id;

    RETURN FOUND;
END;
$$;

--#endregion
