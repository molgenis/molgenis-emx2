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

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT
            table_schema, table_name, constraint_name
        FROM information_schema.table_constraints
        WHERE constraint_type = 'FOREIGN KEY'
            AND table_schema NOT LIKE 'pg_%'
            AND table_schema <> 'information_schema'
            AND table_schema <> 'public'
    LOOP
        EXECUTE
            format('ALTER TABLE %I.%I DROP CONSTRAINT %I;',
                r.table_schema, r.table_name, r.constraint_name);
    END LOOP;
END$$;

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