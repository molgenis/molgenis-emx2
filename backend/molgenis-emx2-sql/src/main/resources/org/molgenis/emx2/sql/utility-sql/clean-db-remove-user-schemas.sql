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
        EXECUTE format('DROP SCHEMA "%I" CASCADE;', r.schema_name);
    END LOOP;
END$$;