DO $$
    DECLARE
        rec RECORD;
        alter_stmt TEXT;
    BEGIN
        FOR rec IN
            SELECT
                n.nspname AS schema_name,
                p.proname AS function_name,
                pg_get_function_identity_arguments(p.oid) AS args
            FROM
                pg_proc p
                    JOIN pg_namespace n ON p.pronamespace = n.oid
                    JOIN pg_language l ON p.prolang = l.oid
            WHERE
                p.prorettype = 'pg_catalog.trigger'::regtype
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', '_SYSTEM_', 'MOLGENIS')
            LOOP
                alter_stmt := format(
                        'ALTER FUNCTION %I.%I(%s) OWNER TO %I;',
                        rec.schema_name,
                        rec.function_name,
                        rec.args,
                        'MG_ROLE_' || rec.schema_name || '/Manager'
                              );

                RAISE NOTICE 'Executing: %', alter_stmt;
                EXECUTE alter_stmt;
            END LOOP;
    END $$;
