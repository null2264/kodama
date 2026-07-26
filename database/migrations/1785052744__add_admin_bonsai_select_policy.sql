-- Creation Date: 2026-07-26 07:59:04.586164 +0000 UTC 
-- Reason: add_admin_bonsai_select_policy 

-- Admin can see all bonsai
CREATE POLICY "Admins can view all bonsai." ON kodama.bonsai
FOR SELECT USING (kodama.is_admin());

-- Admin can see all bonsai metadata
CREATE POLICY "Admins can view all bonsai metadata." ON kodama.bonsai_metadata
FOR SELECT USING (kodama.is_admin());

-- RPC: Get all bonsai with metadata for a contest (bypasses RLS via SECURITY DEFINER)
CREATE OR REPLACE FUNCTION kodama.get_bonsai_with_metadata(p_contest_id uuid)
RETURNS TABLE(
    id uuid,
    name text,
    owner_id uuid,
    contest_id uuid,
    contest_class_id uuid,
    created_at timestamptz,
    pict_path text,
    state kodama.bonsai_state
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = 'kodama'
AS $$
  SELECT b.id, b.name, b.owner_id, b.contest_id, b.contest_class_id,
         b.created_at, b.pict_path, m.state
  FROM kodama.bonsai b
  JOIN kodama.bonsai_metadata m ON b.id = m.id
  WHERE b.contest_id = p_contest_id
  ORDER BY b.created_at;
$$;
