-- Creation Date: 2025-09-24 11:58:57.768505+00:00 UTC
-- Reason: init

-- NOTE: To self -> UPSERT requires policy for SELECT, UPDATE, and INSERT
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
