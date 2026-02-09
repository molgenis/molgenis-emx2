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