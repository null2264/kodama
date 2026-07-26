-- Creation Date: 2026-07-26 08:28:42.633926 +0000 UTC 
-- Reason: add payment proof path 

-- Add payment_proof_path column to bonsai
ALTER TABLE kodama.bonsai
    ADD COLUMN IF NOT EXISTS payment_proof_path text;

-- RPC: Set payment proof path for a bonsai
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

-- RPC: Get all bonsai with metadata for a contest (updated to include payment_proof_path)
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
       b.created_at, b.pict_path, m.state, b.payment_proof_path
FROM kodama.bonsai b
         JOIN kodama.bonsai_metadata m ON b.id = m.id
WHERE b.contest_id = p_contest_id
ORDER BY b.created_at;
$$;
