-- Creation Date: 2025-09-25 03:27:51.254436+00:00 UTC
-- Reason: bonsai classes

CREATE TABLE kodama.bonsai_classes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL UNIQUE, -- "Shohin", "Chuhin", etc. must be unique
    description text
    -- TODO: Maybe later?
    -- created_at timestamptz NOT NULL DEFAULT now(),
    -- updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE kodama.bonsai_classes ENABLE ROW LEVEL SECURITY;

INSERT INTO kodama.bonsai_classes (id, name)
VALUES
('8f8e46fd-75e9-443e-a1f2-7ffab68ece31', 'Prospek'),
('fb85ea10-0fc5-40b9-9e27-f236962c8271', 'Pratama'),
('11943061-aa9d-4cc0-90f6-2ca7b70bc1b5', 'Madya');

-- RLS: Only admins should manage the master list of classes.
CREATE POLICY "Admins can manage master bonsai classes." ON kodama.bonsai_classes
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Everyone can read the class definitions.
CREATE POLICY "All users can view master bonsai classes." ON kodama.bonsai_classes
FOR SELECT USING (true);
