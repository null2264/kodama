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

-- REF: https://supabase.com/docs/guides/troubleshooting/pgrst106-the-schema-must-be-one-of-the-following-error-when-querying-an-exposed-schema
ALTER ROLE authenticator SET pgrst.db_schemas = 'public,kodama';
--#endregion

--#region Enums / Custom Types
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

-- Role is stored in auth.users.raw_app_meta_data (JWT), single source of truth

-- Set default role on user creation
CREATE OR REPLACE FUNCTION kodama.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  UPDATE auth.users
  SET raw_app_meta_data = raw_app_meta_data || '{"role": "user"}'::jsonb
  WHERE id = NEW.id;
  RETURN NEW;
END;
$$;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE PROCEDURE kodama.handle_new_user();

-- Reads role from app_metadata (JWT)
CREATE OR REPLACE FUNCTION kodama.is_admin()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    RETURN (
        SELECT (raw_app_meta_data ->> 'role') = 'admin'
        FROM auth.users WHERE id = auth.uid()
    );
END;
$$;

-- Reads role from app_metadata (JWT)
CREATE OR REPLACE FUNCTION kodama.is_superuser()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    RETURN (
        SELECT (raw_app_meta_data ->> 'role') = 'superuser'
        FROM auth.users WHERE id = auth.uid()
    );
END;
$$;
