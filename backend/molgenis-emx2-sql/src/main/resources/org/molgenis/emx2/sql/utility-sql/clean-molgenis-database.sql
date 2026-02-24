
-- Remove all foreign key constraints that are there in the database, so we can drop the schemas
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT
            tc.table_schema,
            tc.table_name,
            tc.constraint_name
        FROM
            information_schema.table_constraints tc
        WHERE
            tc.constraint_type = 'FOREIGN KEY'
            AND NOT (
                tc.table_schema LIKE 'pg_%'
                OR tc.table_schema = 'information_schema'
                OR tc.table_schema = 'public'
            )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I;',
            r.table_schema, r.table_name, r.constraint_name
        );
    END LOOP;
END $$;

-- Drop the schemas, except for the postgres defaults
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE
            NOT (schema_name LIKE 'pg_%'
                OR schema_name = 'information_schema'
                OR schema_name = 'public')
    LOOP
        EXECUTE format('DROP SCHEMA %I CASCADE;', r.schema_name);
    END LOOP;
END $$;

-- Remove all users.
DO $$
DECLARE
    r RECORD;
    dbname TEXT;
BEGIN
    SELECT current_database() INTO dbname;
    FOR r IN
        SELECT rolname
        FROM pg_roles
        WHERE
            rolname LIKE 'MG\_%' ESCAPE '\'
            OR rolname LIKE 'test%'
            OR rolname LIKE 'user_%'
    LOOP
        -- Revoke all privileges on the database
        EXECUTE format('REVOKE ALL PRIVILEGES ON DATABASE %I FROM %I;', dbname, r.rolname);

        -- Drop the role
        EXECUTE format('DROP ROLE %I;', r.rolname);
    END LOOP;
END $$;