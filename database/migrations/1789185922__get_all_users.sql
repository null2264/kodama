-- Creation Date: 2026-09-12 04:05:22.273438 +0000 UTC
-- Reason: get all users

CREATE OR REPLACE FUNCTION kodama.get_all_users()
RETURNS TABLE(user_id uuid, email text, role text, created_at timestamptz)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT u.id AS user_id, u.email, u.raw_app_meta_data ->> 'role' AS role, u.created_at
  FROM auth.users u
  WHERE kodama.is_admin()
  ORDER BY u.created_at;
$$;

GRANT EXECUTE ON FUNCTION kodama.get_all_users() TO authenticated;
