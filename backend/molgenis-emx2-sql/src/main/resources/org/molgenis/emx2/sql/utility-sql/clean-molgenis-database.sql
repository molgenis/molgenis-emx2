DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT g.rolname AS granted, m.rolname AS member
        FROM pg_auth_members am
        JOIN pg_roles g ON g.oid = am.roleid
        JOIN pg_roles m ON m.oid = am.member
        WHERE g.rolname LIKE 'MG\_%' ESCAPE '\'
            OR g.rolname LIKE 'test%'
            OR g.rolname LIKE 'user_%'
        LOOP
            EXECUTE format('REVOKE %I FROM %I;', r.granted, r.member);
        END LOOP;
END$$;

DO $$
DECLARE
    r RECORD;
    dbname TEXT := current_database();
BEGIN
    FOR r IN
        SELECT rolname, oid
        FROM pg_roles
        WHERE rolname LIKE 'MG\_%' ESCAPE '\'
            OR rolname LIKE 'test%'
            OR rolname LIKE 'user_%'
        LOOP
            EXECUTE format('REVOKE ALL PRIVILEGES ON DATABASE %I FROM %I;', dbname, r.rolname);
            EXECUTE format('DROP OWNED BY %I CASCADE;', r.rolname);
            EXECUTE format('DROP ROLE %I;', r.rolname);
            COMMIT;
        END LOOP;
END$$;

DROP SCHEMA IF EXISTS MOLGENIS CASCADE;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name NOT LIKE 'pg_%'
          AND schema_name <> 'information_schema'
          AND schema_name <> 'public'
        LOOP
            EXECUTE format('DROP SCHEMA %I CASCADE;', r.schema_name);
            COMMIT;
        END LOOP;
END$$;
