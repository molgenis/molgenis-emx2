DO $$
DECLARE
    r RECORD;
    dbname TEXT := current_database();
BEGIN
    FOR r IN
        SELECT rolname
        FROM pg_roles
        WHERE rolname LIKE 'MG\_%' ESCAPE '\'
           OR rolname LIKE 'test%'
           OR rolname LIKE 'user_%'
    LOOP
        EXECUTE format('REVOKE ALL PRIVILEGES ON DATABASE %I FROM %I;', dbname, r.rolname);
        EXECUTE format('DROP ROLE %I;', r.rolname);
    END LOOP;
END$$;