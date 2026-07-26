-- Creation Date: 2026-07-26 14:43:19.600515 +0000 UTC 
-- Reason: find user by email 

CREATE OR REPLACE FUNCTION kodama.find_user_by_email(p_email text)
RETURNS uuid
LANGUAGE sql
SECURITY DEFINER
AS $$
  SELECT id FROM auth.users WHERE email = p_email LIMIT 1;
$$;

GRANT EXECUTE ON FUNCTION kodama.find_user_by_email(text) TO authenticated;
