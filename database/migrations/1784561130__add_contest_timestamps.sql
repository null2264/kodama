-- Creation Date: 2026-07-20 15:25:30.303877 +0000 UTC
-- Reason: add contest timestamps

ALTER TABLE kodama.contests ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE kodama.contests ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

CREATE TRIGGER set_contests_updated_at
BEFORE UPDATE ON kodama.contests
FOR EACH ROW
EXECUTE FUNCTION kodama.set_current_timestamp_updated_at(); 

