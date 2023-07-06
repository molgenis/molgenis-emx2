DO
$$
DECLARE
schemaname name;
BEGIN
FOR schemaname IN SELECT schema_name FROM information_schema.schemata WHERE schema_name <> 'MOLGENIS' AND schema_name <> 'information_schema' AND schema_name <> 'pg_catalog' AND schema_name <> 'public' LOOP
    BEGIN
        EXECUTE 'CREATE ROLE "MG_ROLE_' || schemaname || '/Aggregator"';
        EXECUTE 'GRANT USAGE ON SCHEMA "' || schemaname || '" TO "MG_ROLE_' || schemaname || '/Aggregator"';
    EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
    END;
END LOOP;
END;
$$ LANGUAGE plpgsql;