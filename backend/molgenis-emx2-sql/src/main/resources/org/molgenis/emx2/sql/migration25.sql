-- Function to create or update schema-level permissions
CREATE OR REPLACE FUNCTION "MOLGENIS".create_permissions(
    schema_id TEXT
)
    RETURNS void
    LANGUAGE plpgsql AS
$$
DECLARE
    default_permissions TEXT[] := ARRAY ['SELECT', 'INSERT', 'UPDATE','DELETE','AGG','AGG_COUNT','AGG_RANGE','AGG_EXIST','MANAGE','ADMIN','GROUP_SELECT','GROUP_UPDATE','GROUP_DELETE'];
    default_groups      TEXT[] := ARRAY ['VIEWERS', 'EDITORS', 'MANAGERS', 'ADMIN', 'GROUP_EDITORS', 'GROUP_VIEWERS'];
    permission_name     TEXT;
    group_name          TEXT;
    table_ids           TEXT[];
    table_id            TEXT;
BEGIN
    -- apply to all tables
    SELECT ARRAY_AGG(table_name)
    INTO table_ids
    FROM information_schema.tables
    WHERE table_schema = schema_id
      AND table_type = 'BASE TABLE';

    -- Make sure main admin group exists
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_GROUP:_ADMIN_') THEN
        EXECUTE 'CREATE ROLE "MG_GROUP:_ADMIN_"'; -- super admin
    END IF;

    -- define default permissions
    FOREACH permission_name IN ARRAY default_permissions
        LOOP
            permission_name := format('MG_PERM:%s:_ALL_:%s', schema_id, permission_name);
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = permission_name) THEN
                EXECUTE format('CREATE ROLE %I', permission_name);
            END IF;
        END LOOP;

    -- define default groups
    FOREACH group_name IN ARRAY default_groups
        LOOP
            group_name := format('MG_GROUP:%s_%s', schema_id, group_name);
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = group_name) THEN
                EXECUTE format('CREATE ROLE %I', group_name);
            END IF;
        END LOOP;

    -- Grant usage on the schema
    EXECUTE format(
            'GRANT USAGE ON SCHEMA %1$I TO "MG_PERM:%1$s:_ALL_:SELECT", "MG_PERM:%1$s:_ALL_:GROUP_SELECT", "MG_PERM:%1$s:_ALL_:INSERT", "MG_PERM:%1$s:_ALL_:UPDATE", "MG_PERM:%1$s:_ALL_:GROUP_UPDATE",
            "MG_PERM:%1$s:_ALL_:DELETE", "MG_PERM:%1$s:_ALL_:GROUP_DELETE", "MG_PERM:%1$s:_ALL_:ADMIN"',
            schema_id
            );

    -- Grant CREATE privilege on the schema
    EXECUTE format(
            'GRANT CREATE ON SCHEMA %1$I TO "MG_PERM:%1$s:_ALL_:ADMIN" WITH GRANT OPTION',
            schema_id
            );


    -- Grant SELECT/INSERT/UPDATE/DELETE to admin
    EXECUTE format(
            'GRANT "MG_PERM:%1$s:_ALL_:SELECT", "MG_PERM:%1$s:_ALL_:INSERT", "MG_PERM:%1$s:_ALL_:UPDATE", "MG_PERM:%1$s:_ALL_:DELETE" TO "MG_PERM:%1$s:_ALL_:ADMIN" WITH ADMIN OPTION',
            schema_id
            );


    -- Grant SELECT permissions to aggregate roles
    EXECUTE format(
            'GRANT "MG_PERM:%1$s:_ALL_:SELECT" TO "MG_PERM:%1$s:_ALL_:AGG", "MG_PERM:%1$s:_ALL_:AGG_COUNT", "MG_PERM:%1$s:_ALL_:AGG_RANGE", "MG_PERM:%1$s:_ALL_:AGG_EXIST"',
            schema_id
            );

    -- Grant to groups
    EXECUTE format('GRANT "MG_PERM:%1$s:_ALL_:SELECT" TO "MG_GROUP:%1$s_VIEWERS"', schema_id);
    EXECUTE format('GRANT "MG_PERM:%1$s:_ALL_:GROUP_SELECT" TO "MG_GROUP:%1$s_GROUP_VIEWERS"', schema_id);
    EXECUTE format(
            'GRANT "MG_PERM:%1$s:_ALL_:SELECT", "MG_PERM:%1$s:_ALL_:INSERT", "MG_PERM:%1$s:_ALL_:UPDATE", "MG_PERM:%1$s:_ALL_:DELETE" TO "MG_GROUP:%1$s_EDITORS"',
            schema_id);
    EXECUTE format(
            'GRANT "MG_PERM:%1$s:_ALL_:GROUP_SELECT", "MG_PERM:%1$s:_ALL_:INSERT", "MG_PERM:%1$s:_ALL_:GROUP_UPDATE", "MG_PERM:%1$s:_ALL_:GROUP_DELETE" TO "MG_GROUP:%1$s_GROUP_EDITORS"',
            schema_id);
    EXECUTE format(
            'GRANT "MG_PERM:%1$s:_ALL_:ADMIN", "MG_GROUP:%1$s_VIEWERS", "MG_GROUP:%1$s_EDITORS", "MG_GROUP:%1$s_GROUP_VIEWERS", "MG_GROUP:%1$s_GROUP_EDITORS" TO "MG_GROUP:%1$s_ADMIN" WITH ADMIN OPTION',
            schema_id);

    FOREACH table_id IN ARRAY table_ids
        LOOP
            -- create table level permissions
            FOREACH permission_name IN ARRAY default_permissions
                LOOP
                    permission_name := format('MG_PERM:%s:%s:%s', schema_id, table_id, permission_name);
                    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = permission_name) THEN
                        EXECUTE format('CREATE ROLE %I', permission_name);
                    END IF;
                END LOOP;
            EXECUTE format(
                    'GRANT SELECT ON TABLE %1$I.%2$I TO "MG_PERM:%1$s:_ALL_:SELECT", "MG_PERM:%1$s:%2$s:SELECT", "MG_PERM:%1$s:_ALL_:GROUP_SELECT", "MG_PERM:%1$s:%2$s:GROUP_SELECT"',
                    schema_id, table_id);
            EXECUTE format(
                    'GRANT INSERT ON TABLE %1$I.%2$I TO "MG_PERM:%1$s:_ALL_:INSERT", "MG_PERM:%1$s:%2$s:INSERT"',
                    schema_id, table_id);
            EXECUTE format(
                    'GRANT UPDATE ON TABLE %1$I.%2$I TO "MG_PERM:%1$s:_ALL_:UPDATE", "MG_PERM:%1$s:%2$s:UPDATE", "MG_PERM:%1$s:_ALL_:GROUP_UPDATE", "MG_PERM:%1$s:%2$s:GROUP_UPDATE"',
                    schema_id, table_id);
            EXECUTE format(
                    'GRANT DELETE ON TABLE %1$I.%2$I TO "MG_PERM:%1$s:_ALL_:DELETE", "MG_PERM:%1$s:_ALL_:GROUP_DELETE", "MG_PERM:%1$s:%2$s:DELETE", "MG_PERM:%1$s:%2$s:GROUP_DELETE"',
                    schema_id, table_id);
            -- Below are protected in middleware; would require views to protect on backend
            EXECUTE format(
                    'GRANT "MG_PERM:%1$s:%2$s:SELECT" TO "MG_PERM:%1$s:%2$s:AGG", "MG_PERM:%1$s:%2$s:AGG_COUNT", "MG_PERM:%1$s:%2$s:AGG_RANGE", "MG_PERM:%1$s:%2$s:AGG_EXIST"',
                    schema_id, table_id);

            -- ensure the schema admin can grant these roles
            EXECUTE format(
                    'GRANT "MG_PERM:%1$s:%2$s:SELECT", "MG_PERM:%1$s:%2$s:INSERT", "MG_PERM:%1$s:%2$s:UPDATE", "MG_PERM:%1$s:%2$s:DELETE" TO "MG_PERM:%1$s:_ALL_:ADMIN" WITH ADMIN OPTION',
                    schema_id, table_id);

            EXECUTE format('ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS mg_group VARCHAR', schema_id, table_id);
            EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY',schema_id, table_id);
            EXECUTE format(
                    'CREATE POLICY select_policy ON %I.%I FOR SELECT USING (pg_has_role(''MG_PERM:%s:%s:SELECT'',current_user,''MEMBER'') OR (pg_has_role(''MG_PERM:%s:%s:GROUP_SELECT'',''MEMBER'') AND pg_has_role(mg_group, current_user,''MEMBER'')))',
                    schema_id, table_id, schema_id, table_id, schema_id, table_id
                    );
        END LOOP;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Error during GRANT execution: %', SQLERRM;
    RAISE;
END
$$;

-- test on the pet store
SELECT "MOLGENIS"."create_permissions"('pet store');
