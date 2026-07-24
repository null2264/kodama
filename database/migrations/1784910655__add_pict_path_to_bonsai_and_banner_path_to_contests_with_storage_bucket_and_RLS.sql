-- Creation Date: 2026-07-24 16:30:55.41588 +0000 UTC 
-- Reason: add pict_path to bonsai and banner_path to contests with storage bucket and RLS 

--#region Add image path columns

ALTER TABLE kodama.bonsai
ADD COLUMN pict_path text;

ALTER TABLE kodama.contests
ADD COLUMN banner_path text;

--#endregion

--#region Create storage bucket

INSERT INTO storage.buckets (id, name, public)
VALUES ('kodama-images', 'kodama-images', true)
ON CONFLICT (id) DO NOTHING;

--#endregion

--#region RLS Policies for storage.objects on kodama-images bucket

-- Grant usage on storage schema for authenticated users
GRANT USAGE ON SCHEMA storage TO authenticated;
GRANT ALL ON storage.objects TO authenticated;

-- Allow authenticated users to SELECT any object in kodama-images
CREATE POLICY "Authenticated users can read images" ON storage.objects
FOR SELECT TO authenticated
USING (bucket_id = 'kodama-images');

-- Allow users to upload bonsai picts they own
CREATE POLICY "Owner can upload bonsai pict" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'kodama-images'
    AND kodama.is_bonsai_owner(
        -- Extract bonsai_id from the path: bonsai/{uuid}/...
        (string_to_array(name, '/'))[2]::uuid,
        auth.uid()
    )
);

-- Allow admins to upload contest banners
CREATE POLICY "Admin can upload contest banner" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'kodama-images'
    AND kodama.is_admin()
);

-- Allow owner or admin to update/delete objects
CREATE POLICY "Owner or admin can manage images" ON storage.objects
FOR ALL TO authenticated
USING (
    bucket_id = 'kodama-images'
    AND (
        kodama.is_admin()
        OR (
            -- Check ownership via path pattern: bonsai/{bonsai_id}/... or contest/{contest_id}/...
            EXISTS (
                SELECT 1 FROM kodama.bonsai
                WHERE id = (string_to_array(name, '/'))[2]::uuid
                  AND owner_id = auth.uid()
            )
        )
    )
);

--#endregion

--#region Helper functions

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

-- Helper to set bonsai pict path (only owner can do this)
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

-- Helper to set contest banner path (only admin can do this)
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