ALTER POLICY "schema_metadata_POLICY" ON "MOLGENIS"."schema_metadata" USING (pg_has_role(
        (concat('MG_ROLE_', table_schema, '/Exists'))::name, 'MEMBER'::text));
DO
$$
    DECLARE
        schemaname name;
        tablename  name;
    BEGIN
        FOR schemaname IN SELECT schema_name
                          FROM information_schema.schemata
                          WHERE schema_name <> 'MOLGENIS'
                            AND schema_name <> 'information_schema'
                            AND schema_name <> 'pg_catalog'
                            AND schema_name <> 'public'
                            AND schema_name <> 'pg_toast'
                            AND schema_name NOT LIKE 'pg_%'
            LOOP
                BEGIN
                    EXECUTE 'CREATE ROLE "MG_ROLE_' || schemaname || '/Exists"';
                    EXECUTE 'CREATE ROLE "MG_ROLE_' || schemaname || '/Range"';
                    EXECUTE 'CREATE ROLE "MG_ROLE_' || schemaname || '/Count"';
                    EXECUTE 'GRANT USAGE ON SCHEMA "' || schemaname || '" TO "MG_ROLE_' || schemaname || '/Exists"';
                    EXECUTE 'GRANT USAGE ON SCHEMA "' || schemaname || '" TO "MG_ROLE_' || schemaname || '/Range"';
                    EXECUTE 'GRANT USAGE ON SCHEMA "' || schemaname || '" TO "MG_ROLE_' || schemaname || '/Count"';
                    EXECUTE 'GRANT "MG_ROLE_' || schemaname || '/Exists" TO "MG_ROLE_' || schemaname || '/Range"';
                    EXECUTE 'GRANT "MG_ROLE_' || schemaname || '/Range" TO "MG_ROLE_' || schemaname || '/Aggregator"';
                    EXECUTE 'GRANT "MG_ROLE_' || schemaname || '/Count" TO "MG_ROLE_' || schemaname || '/Viewer"';
                    FOR tablename IN SELECT table_name FROM information_schema.tables WHERE table_schema = schemaname
                        LOOP
                            BEGIN
                                EXECUTE 'GRANT SELECT ON "' || schemaname || '"."' || tablename || '" TO "MG_ROLE_' ||
                                        schemaname || '/Exists"';
                            END;
                        END LOOP;
                EXCEPTION
                    WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
                END;
            END LOOP;
    END;
$$ LANGUAGE plpgsql;
