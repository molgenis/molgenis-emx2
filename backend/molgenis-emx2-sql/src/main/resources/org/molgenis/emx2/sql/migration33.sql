-- Introduce the system role 'Using' for every schema.
--
-- Until now custom roles were made a member of 'Exists' so they could see and use their schema.
-- That made 'Exists' ambiguous: it meant both "may use this schema" and "may run exists/aggregate
-- queries". Row level security needs to tell those apart, so that aggregate permissions can bypass
-- the row policies while custom (row level) roles cannot. 'Using' now carries the schema usage,
-- 'Exists' only the aggregate permission.
DO
$$
    DECLARE
        schemaname name;
        rolename   name;
        tablename  name;
        prefix     text;
    BEGIN
        FOR schemaname IN SELECT left(right(rolname, -length('MG_ROLE_')), -length('/Exists'))
                          FROM pg_roles
                          WHERE rolname LIKE 'MG\_ROLE\_%/Exists'
            LOOP
                prefix := 'MG_ROLE_' || schemaname || '/';

                IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = prefix || 'Using') THEN
                    EXECUTE format('CREATE ROLE %I WITH NOLOGIN', prefix || 'Using');
                END IF;

                IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = schemaname) THEN
                    EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I', schemaname, prefix || 'Using');
                    -- custom roles used to reach the tables through 'Exists', now through 'Using'
                    FOR tablename IN SELECT c.relname
                                     FROM pg_class c
                                              JOIN pg_namespace n ON n.oid = c.relnamespace
                                     WHERE n.nspname = schemaname
                                       AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
                        LOOP
                            EXECUTE format('GRANT SELECT ON %I.%I TO %I', schemaname, tablename,
                                           prefix || 'Using');
                        END LOOP;
                END IF;

                -- every system role reaches 'Using' through 'Exists'
                EXECUTE format('GRANT %I TO %I', prefix || 'Using', prefix || 'Exists');
                EXECUTE format('GRANT %I TO %I WITH ADMIN OPTION', prefix || 'Using', prefix || 'Manager');
                EXECUTE format('GRANT %I TO %I WITH ADMIN OPTION', prefix || 'Using', prefix || 'Owner');

                -- move custom roles (including the internal RLS_ proxies) from 'Exists' to 'Using'
                FOR rolename IN SELECT r.rolname
                                FROM pg_roles r
                                WHERE left(r.rolname, length(prefix)) = prefix
                                  AND right(r.rolname, -length(prefix)) NOT IN
                                      ('Using', 'Exists', 'Range', 'Aggregator', 'Count', 'Viewer',
                                       'Editor', 'Manager', 'Owner')
                    LOOP
                        EXECUTE format('GRANT %I TO %I', prefix || 'Using', rolename);
                        EXECUTE format('REVOKE %I FROM %I', prefix || 'Exists', rolename);
                    END LOOP;
            END LOOP;
    END;
$$ LANGUAGE plpgsql;

-- schema visibility is now driven by 'Using' instead of 'Exists'
ALTER POLICY "schema_metadata_POLICY" ON "MOLGENIS"."schema_metadata" USING (pg_has_role(
        (concat('MG_ROLE_', table_schema, '/Using'))::name, 'MEMBER'::text));

-- Add the aggregate bypass policy to tables that already have row level security enabled, and drop
-- the read bypass for 'Viewer': every role from 'Viewer' up is a member of 'Exists' through the role
-- chain, so the aggregate bypass (also SELECT only) already covers it.
DO
$$
    DECLARE
        policyrow record;
    BEGIN
        FOR policyrow IN SELECT schemaname, tablename
                         FROM pg_policies
                         WHERE policyname = 'mg_roles_row_match'
            LOOP
                EXECUTE format('DROP POLICY IF EXISTS mg_roles_viewer_bypass ON %I.%I',
                               policyrow.schemaname, policyrow.tablename);
                IF NOT EXISTS (SELECT 1
                               FROM pg_policies p
                               WHERE p.schemaname = policyrow.schemaname
                                 AND p.tablename = policyrow.tablename
                                 AND p.policyname = 'mg_roles_aggregate_bypass') THEN
                    EXECUTE format(
                            'CREATE POLICY mg_roles_aggregate_bypass ON %I.%I FOR SELECT USING (pg_has_role(current_user, %L, ''member''))',
                            policyrow.schemaname, policyrow.tablename,
                            'MG_ROLE_' || policyrow.schemaname || '/Exists');
                END IF;
            END LOOP;
    END;
$$ LANGUAGE plpgsql;
