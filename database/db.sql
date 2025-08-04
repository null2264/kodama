CREATE SCHEMA kodama;

CREATE TYPE kodama.role AS ENUM (
    'user',
    'admin',
);
CREATE TYPE kodama.contest_role AS ENUM (
    'contestant',
    -- TODO:
    -- Judges are bound to their respective bonsai classes
    'judge',
    -- TODO:
    -- Head Judge has extra privilege to accept or deny Judge's request for things like revising a review.
    'head_judge',
);

-- TODO: Handle 2 phase review
CREATE TYPE kodama.bonsai_state AS ENUM (
    'draft',
    'waiting_payment',
    -- Admin need to verify the payment manually
    'waiting_verify',
    'verified'
);


CREATE TYPE kodama.contest_state AS ENUM (
    'draft',
    'accepting',
    'reviewing',
    'finished'
);


CREATE TABLE kodama.bonsai (
    id uuid NOT NULL,
    name text,
    owner_id uuid NOT NULL,
    contest_id uuid NOT NULL,
    state kodama.bonsai_state NOT NULL
);

CREATE POLICY "Users can add bonsai." ON kodama.bonsai
FOR INSERT TO authenticated
WITH CHECK ((( SELECT auth.uid() AS uid) = owner_id));

CREATE POLICY "Users can see their own bonsai." ON kodama.bonsai
FOR SELECT
USING ((( SELECT auth.uid() AS uid) = owner_id));

CREATE POLICY "Users can update their own bonsai." ON kodama.bonsai
FOR UPDATE TO authenticated
USING ((( SELECT auth.uid() AS uid) = owner_id))
WITH CHECK ((( SELECT auth.uid() AS uid) = owner_id));


CREATE TABLE kodama.contests (
    id uuid NOT NULL,
    name text NOT NULL,
    description text,
    state kodama.contest_state NOT NULL
);

-- FIXME: Only allow admin to create contests for now
CREATE POLICY "Privileged users can create contests." ON kodama.contests
FOR INSERT TO authenticated
WITH CHECK (true);  -- TODO: Replace 'true' with "is admin" check
