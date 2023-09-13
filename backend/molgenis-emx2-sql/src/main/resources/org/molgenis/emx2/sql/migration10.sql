DO
$$
DECLARE
schemaname name;
tablename name;
BEGIN
FOR schemaname IN SELECT schema_name FROM information_schema.schemata WHERE schema_name <> 'MOLGENIS' AND schema_name <> 'information_schema' AND schema_name <> 'pg_catalog' AND schema_name <> 'public' LOOP
    BEGIN
        EXECUTE 'CREATE ROLE "MG_ROLE_' || schemaname || '/Aggregator"';
        EXECUTE 'GRANT USAGE ON SCHEMA "' || schemaname || '" TO "MG_ROLE_' || schemaname || '/Aggregator"';
        EXECUTE 'GRANT "MG_ROLE_' || schemaname || '/Aggregator" TO "MG_ROLE_' || schemaname || '/Viewer"';
        FOR tablename IN SELECT table_name FROM information_schema.tables WHERE table_schema = schemaname LOOP
        BEGIN
            EXECUTE 'GRANT SELECT ON "' || schemaname || '"."' || tablename || '" TO "MG_ROLE_' || schemaname || '/Aggregator"';
        END;
        END LOOP;
    EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
    END;
END LOOP;
END;
$$ LANGUAGE plpgsql;