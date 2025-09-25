-- Creation Date: 2025-09-25 03:28:33.898604+00:00 UTC
-- Reason: contests

CREATE TABLE kodama.contests (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name text NOT NULL,
    description text,
    state kodama.contest_state NOT NULL DEFAULT 'draft'
);

ALTER TABLE kodama.contests ENABLE ROW LEVEL SECURITY;

-- Admins can do anything on contests.
CREATE POLICY "Admins can manage contests." ON kodama.contests
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Authenticated users can see contests that are not in 'draft' state.
CREATE POLICY "Authenticated users can view active contests." ON kodama.contests
FOR SELECT TO authenticated
USING (state <> 'draft');

CREATE OR REPLACE FUNCTION kodama.is_registration_open(contest_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = 'kodama'
AS $$
BEGIN
    RETURN (
        SELECT state = 'accepting' FROM kodama.contests WHERE id = contest_id
    );
END;
$$;

-- This helps us archive contest classes in case a class is added/removed in the future without affecting old contests.
CREATE TABLE kodama.contest_classes (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    contest_id uuid NOT NULL REFERENCES kodama.contests(id) ON DELETE CASCADE,
    class_id uuid NOT NULL REFERENCES kodama.bonsai_classes(id) ON DELETE RESTRICT,
    UNIQUE(contest_id, class_id)
);

ALTER TABLE kodama.contest_classes ENABLE ROW LEVEL SECURITY;

-- RLS Policies for the junction table
-- Admins can link classes to contests.
CREATE POLICY "Admins can manage contest class offerings." ON kodama.contest_classes
FOR ALL USING (kodama.is_admin()) WITH CHECK (kodama.is_admin());

-- Users can see which classes are offered in an active contest.
CREATE POLICY "Users can view classes for active contests." ON kodama.contest_classes
FOR SELECT TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM kodama.contests c
        WHERE c.id = contest_classes.contest_id AND c.state <> 'draft'
    )
);
