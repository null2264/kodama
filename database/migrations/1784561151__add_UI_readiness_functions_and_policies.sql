-- Creation Date: 2026-07-20 15:25:51.688113 +0000 UTC
-- Reason: add UI readiness functions and policies

--#region RPC: Judges list pending reviews
CREATE OR REPLACE FUNCTION kodama.get_pending_reviews(p_contest_id uuid)
RETURNS TABLE(id uuid, name text, owner_id uuid, contest_class_id uuid, created_at timestamptz)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = 'kodama'
AS $$
  SELECT b.id, b.name, b.owner_id, b.contest_class_id, b.created_at
  FROM kodama.bonsai b
  JOIN kodama.bonsai_metadata m ON b.id = m.id
  WHERE b.contest_id = p_contest_id
    AND m.state = 'verified'
    AND EXISTS (
      SELECT 1 FROM kodama.contest_participants p
      WHERE p.contest_id = b.contest_id
        AND p.user_id = auth.uid()
        AND p.role IN ('judge', 'head_judge')
        AND (p.role = 'head_judge' OR p.contest_class_id = b.contest_class_id)
    )
    AND NOT EXISTS (
      SELECT 1 FROM kodama.reviews r
      WHERE r.bonsai_id = b.id AND r.judge_id = auth.uid()
    )
  ORDER BY b.created_at;
$$;
--#endregion

--#region RLS: Contestants see own bonsai scores in finished/ended contests
CREATE POLICY "Contestants can see reviews on their own bonsai in finished contests." ON kodama.reviews
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM kodama.bonsai b
    WHERE b.id = reviews.bonsai_id
      AND b.owner_id = auth.uid()
      AND EXISTS (
        SELECT 1 FROM kodama.contests c
        WHERE c.id = b.contest_id AND c.state IN ('finished', 'ended')
      )
  )
);
--#endregion

--#region RLS: Admins see user metadata (for user assignment UI)
DROP POLICY IF EXISTS "Superuser can see a user's metadata." ON kodama.user_metadata;
CREATE POLICY "Superuser and admins can see user metadata." ON kodama.user_metadata
FOR SELECT TO authenticated
USING (kodama.is_superuser() OR kodama.is_admin());
--#endregion 

