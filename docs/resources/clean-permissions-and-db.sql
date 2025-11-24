DROP DATABASE IF EXISTS molgenis WITH (FORCE);

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT rolname
        FROM pg_roles
        WHERE rolname LIKE 'MG\_%' ESCAPE '\'
    LOOP
        EXECUTE 'DROP ROLE "' || r.rolname || '";';
    END LOOP;
END$$;

DROP ROLE IF EXISTS "molgenis";

create database molgenis;
create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';
grant all privileges on database molgenis to molgenis;