-- Creation Date: 2025-09-24 11:58:57.768505+00:00 UTC
-- Reason: init

-- NOTE: To self -> UPSERT requires policy for SELECT, UPDATE, and INSERT
CREATE SCHEMA kodama;

--#region Grants bs so supabase/postgrest can stop crying
-- We need to do this otherwise supabase (or rather postgrest) would cry about not having permission
-- REF: https://github.com/supabase/supabase/blob/24ce0ba5f87698ad72c173c7a26fa6c5c105e8ca/docker/volumes/db/webhooks.sql#L5C3-L9C127
GRANT USAGE ON SCHEMA kodama TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA kodama GRANT ALL ON TABLES TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA kodama GRANT ALL ON FUNCTIONS TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA kodama GRANT ALL ON SEQUENCES TO postgres, anon, authenticated, service_role;
--#endregion

--#region Enums / Custom Types
CREATE TYPE kodama.role AS ENUM (
    'superuser',
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
    'closed',
    'reviewing',
    'finished',
    'ended'
);
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

CREATE OR REPLACE FUNCTION kodama.is_superuser()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT role = 'superuser' FROM kodama.user_metadata WHERE id = auth.uid()
    );
END;
$$;

CREATE POLICY "Superuser can update a user's metadata." ON kodama.user_metadata
FOR UPDATE TO authenticated
USING (kodama.is_superuser());
CREATE POLICY "Superuser can see a user's metadata." ON kodama.user_metadata
FOR SELECT TO authenticated
USING (kodama.is_superuser());
