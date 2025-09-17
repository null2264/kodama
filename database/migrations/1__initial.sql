CREATE SCHEMA kodama;

--#region Grants bs so supabase/postgrest can stop crying
-- We need to do this otherwise supabase (or rather postgrest) would cry about not having permission
-- REF: https://github.com/supabase/supabase/blob/a2fc6d592cb4ea50fd518b99db199a31912040b9/docker/volumes/db/init/00-initial-schema.sql#L26-L29
grant usage              on schema kodama to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on tables to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on functions to postgres, anon, authenticated, service_role;
alter default privileges in schema kodama grant all on sequences to postgres, anon, authenticated, service_role;
--#endregion
