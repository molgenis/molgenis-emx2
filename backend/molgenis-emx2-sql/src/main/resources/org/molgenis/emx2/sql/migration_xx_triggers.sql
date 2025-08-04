-- ========================================
-- Tables
-- ========================================

-- Create groups metadata table, which will also trigger group creation, update, drop
CREATE TABLE IF NOT EXISTS "MOLGENIS".group_metadata
(
    group_name        TEXT PRIMARY KEY, -- Friendly name for the group
    group_description TEXT,             -- Optional description of the group
    users             TEXT[]            -- List of users in the group
);
ALTER TABLE "MOLGENIS".group_metadata
    OWNER TO molgenis;
GRANT DELETE, INSERT, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON "MOLGENIS".group_metadata TO PUBLIC;

-- Create groups permissions table
CREATE TABLE IF NOT EXISTS "MOLGENIS".group_permissions
(
    group_name   TEXT    NOT NULL,
    table_schema TEXT    NULL,
    table_name   TEXT    NULL,
    has_select   BOOLEAN NOT NULL DEFAULT FALSE,
    has_insert   BOOLEAN NOT NULL DEFAULT FALSE,
    has_update   BOOLEAN NOT NULL DEFAULT FALSE,
    has_delete   BOOLEAN NOT NULL DEFAULT FALSE,
    has_admin    BOOLEAN NOT NULL DEFAULT FALSE, -- only for schema
    PRIMARY KEY (group_name),
    CONSTRAINT fk_table_metadata FOREIGN KEY (table_schema, table_name)
        REFERENCES "MOLGENIS".table_metadata (table_schema, table_name)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_group_metadata FOREIGN KEY (group_name)
        REFERENCES "MOLGENIS".group_metadata (group_name)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_permissions_lookup ON "MOLGENIS".group_permissions (table_schema, table_name, group_name);
ALTER TABLE "MOLGENIS".group_permissions
    OWNER TO molgenis;
GRANT DELETE, INSERT, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON "MOLGENIS".group_permissions TO PUBLIC;

-- Materialized view for fast lookups user - permission mappings
CREATE MATERIALIZED VIEW IF NOT EXISTS "MOLGENIS".user_permissions_mv AS
SELECT gp.group_name,
       gp.table_schema,
       gp.table_name,
       gp.has_select,
       gp.has_insert,
       gp.has_update,
       gp.has_delete,
       gp.has_admin,
       r.rolname AS user_name
FROM "MOLGENIS".group_permissions gp
         JOIN pg_roles r
              ON pg_has_role(r.rolname, 'MG_ROLE_' || gp.group_name, 'MEMBER')
WHERE r.rolname LIKE 'MG_USER_%'
WITH NO DATA;
ALTER MATERIALIZED VIEW "MOLGENIS".user_permissions_mv
    OWNER TO molgenis;
GRANT SELECT ON "MOLGENIS".user_permissions_mv TO PUBLIC;
REFRESH MATERIALIZED VIEW "MOLGENIS".user_permissions_mv;
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_schema
    ON "MOLGENIS".user_permissions_mv (user_name, table_schema, table_name);

-- ========================================
-- Triggers
-- ========================================

-- Create default group permissions when a schema is added
CREATE OR REPLACE FUNCTION "MOLGENIS".schema_metadata_trigger_function()
    RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN -- TODO: handle update and delete
        PERFORM "MOLGENIS".create_or_update_schema_groups(NEW.table_schema);
    END IF;

    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER schema_metadata_trigger
    AFTER INSERT OR DELETE OR UPDATE
    ON "MOLGENIS".schema_metadata
    FOR EACH ROW
EXECUTE FUNCTION "MOLGENIS".schema_metadata_trigger_function();

-- Create default permissions when a table is added
CREATE OR REPLACE FUNCTION "MOLGENIS".table_metadata_trigger_function()
    RETURNS TRIGGER AS
$$
DECLARE
    rec       RECORD;
    role_name TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        FOR rec IN
            SELECT gp.*, gm.*
            FROM "MOLGENIS".group_permissions gp
                     JOIN "MOLGENIS".group_metadata gm ON gm.group_name = gp.group_name
            WHERE gp.table_schema = NEW.table_schema
              AND gp.table_name IS NULL
            LOOP
                role_name := 'MG_ROLE_' || rec.group_name;

                -- Grant permissions on the newly inserted table
                PERFORM "MOLGENIS".grant_table_permissions(rec, role_name, NEW.table_name);
            END LOOP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER table_metadata_trigger
    AFTER INSERT OR DELETE OR UPDATE
    ON "MOLGENIS".table_metadata
    FOR EACH ROW
EXECUTE FUNCTION "MOLGENIS".table_metadata_trigger_function();


-- Create permission when a new permission group is added
CREATE OR REPLACE FUNCTION "MOLGENIS".group_metadata_trigger_function()
    RETURNS TRIGGER AS
$$
DECLARE
    user_var    TEXT;
    user_exists BOOLEAN;
    role_name   TEXT;
    role_exists BOOLEAN;
BEGIN
    role_name := 'MG_ROLE_' || NEW.group_name;

    -- Handle INSERT operation: when users are added to the group
    IF TG_OP = 'INSERT' THEN

        -- TODO: added this here - for special groups
        SELECT EXISTS (SELECT 1
                       FROM pg_roles
                       WHERE rolname = role_name)
        INTO role_exists;

        IF NOT role_exists THEN
            EXECUTE format('CREATE ROLE %I', role_name);
            RAISE NOTICE 'Role % created', role_name;
        END IF;

        IF NEW.users IS NOT NULL THEN
            FOREACH user_var IN ARRAY NEW.users
                LOOP
                    -- Verify if the user exists in the users_metadata table
                    SELECT EXISTS(SELECT 1 FROM "MOLGENIS".users_metadata WHERE username = user_var)
                    INTO user_exists;

                    IF user_exists THEN
                        -- Grant the group role to the user if they exist
                        EXECUTE format('GRANT %I TO %I', role_name, 'MG_USER_' || user_var);
                        RAISE NOTICE 'Granting role % to user %', role_name, user_var;
                    ELSE
                        -- Log or handle the case where the user doesn't exist
                        RAISE NOTICE 'User % does not exist', user_var;
                    END IF;
                END LOOP;
        END IF;

    -- Handle DELETE operation: when a group is deleted
    ELSIF TG_OP = 'DELETE' THEN
        FOREACH user_var IN ARRAY OLD.users
            LOOP
                -- Revoke the group role from the user
                EXECUTE format('REVOKE %I FROM %I', OLD.group_name, user_var);
            END LOOP;

    -- Handle UPDATE operation: when users are added or removed in the updated array
    ELSIF TG_OP = 'UPDATE' THEN
        -- Grant roles to new users in the updated list
        FOREACH user_var IN ARRAY NEW.users
            LOOP
                -- Verify if the user exists in the users_metadata table
                SELECT EXISTS(SELECT 1 FROM "MOLGENIS".users_metadata WHERE username = user_var) INTO user_exists;
                IF NOT user_exists THEN
                    RAISE NOTICE 'User % does not exist', user_var;
                    -- Grant the role only if the user was not already in the group
                ELSIF NOT user_var = ANY (OLD.users) THEN
                    RAISE NOTICE 'Granting role % to user %', role_name, user_var;
                    EXECUTE format('GRANT %I TO %I', NEW.group_name, 'MG_USER_' || user_var);
                ELSE
                    RAISE NOTICE 'User % already existed in group %', user_var, NEW.group_name; -- TODO: remove me
                END IF;
            END LOOP;

        -- Revoke roles from users no longer in the group
        FOREACH user_var IN ARRAY OLD.users
            LOOP
                IF NOT user_var = ANY (NEW.users) THEN
                    EXECUTE format('REVOKE %I FROM %I', OLD.group_name, user_var);
                END IF;
            END LOOP;


    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER group_metadata_trigger
    AFTER INSERT OR DELETE OR UPDATE
    ON "MOLGENIS".group_metadata
    FOR EACH ROW
EXECUTE FUNCTION "MOLGENIS".group_metadata_trigger_function();

-- Create the grants for when a record is added to group permissions
CREATE OR REPLACE FUNCTION "MOLGENIS".group_permissions_trigger_function()
    RETURNS TRIGGER AS
$$
DECLARE
    table_ids TEXT[];
    table_id  TEXT;
    role_name TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        role_name := 'MG_ROLE_' || NEW.group_name;
        RAISE NOTICE 'Granting USAGE on schema % to role %', NEW.table_schema, role_name;
        EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I', NEW.table_schema, role_name);

        IF NEW.table_name IS NULL THEN
            SELECT ARRAY_AGG(table_name)
            INTO table_ids
            FROM "MOLGENIS".table_metadata
            WHERE table_schema = NEW.table_schema
              AND table_type = 'BASE TABLE';

            IF table_ids IS NOT NULL THEN
                FOREACH table_id IN ARRAY table_ids
                    LOOP
                        PERFORM "MOLGENIS".grant_table_permissions(NEW, role_name, table_id);
                    END LOOP;
            END IF;
        ELSE
            PERFORM "MOLGENIS".grant_table_permissions(NEW, role_name, NEW.table_name);
        END IF;

    ELSIF TG_OP = 'DELETE' THEN
        role_name := OLD.group_name;
        RAISE NOTICE 'Revoking permissions for group % on schema %', role_name, OLD.table_schema;

        IF OLD.table_name IS NULL THEN
            SELECT ARRAY_AGG(table_name)
            INTO table_ids
            FROM "MOLGENIS".table_metadata
            WHERE table_schema = OLD.table_schema;

            IF OLD.has_admin THEN
                RAISE NOTICE 'Revoking CREATE on schema % from %', OLD.table_schema, role_name;
                EXECUTE format('REVOKE CREATE ON SCHEMA %I FROM %I', OLD.table_schema, role_name);
            END IF;

            IF table_ids IS NOT NULL THEN
                FOREACH table_id IN ARRAY table_ids
                    LOOP
                        PERFORM "MOLGENIS".revoke_table_permissions(OLD, role_name, table_id);
                    END LOOP;
            END IF;
        ELSE
            PERFORM "MOLGENIS".revoke_table_permissions(OLD, role_name, OLD.table_name);
        END IF;

        -- Revoke USAGE if no remaining permissions
        PERFORM 1
        FROM "MOLGENIS".group_permissions
        WHERE group_name = OLD.group_name
          AND table_schema = OLD.table_schema;

        IF NOT FOUND THEN
            RAISE NOTICE 'Revoking USAGE on schema % from group %', OLD.table_schema, OLD.group_name;
            EXECUTE format('REVOKE USAGE ON SCHEMA %I FROM %I', OLD.table_schema, OLD.group_name);
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER group_permissions_trigger
    AFTER INSERT OR DELETE OR UPDATE
    ON "MOLGENIS".group_permissions
    FOR EACH ROW
EXECUTE FUNCTION "MOLGENIS".group_permissions_trigger_function();

-- Trigger for updating materialized view
CREATE OR REPLACE FUNCTION "MOLGENIS".user_permissions_mv_trigger_function()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Refresh the materialized view whenever the group_permissions table changes
    REFRESH MATERIALIZED VIEW "MOLGENIS".user_permissions_mv;
    RETURN NEW; -- Continue with the operation
END;
$$ LANGUAGE plpgsql;
CREATE OR REPLACE TRIGGER user_permissions_mv_trigger
    AFTER INSERT OR UPDATE OR DELETE
    ON "MOLGENIS".group_permissions
    FOR EACH STATEMENT
EXECUTE FUNCTION "MOLGENIS".user_permissions_mv_trigger_function();

-- ========================================
-- Helper functions
-- ========================================

CREATE OR REPLACE FUNCTION "MOLGENIS".grant_table_permissions(
    permissions RECORD,
    role_name TEXT,
    table_name TEXT
)
    RETURNS VOID AS
$$
BEGIN
    IF permissions.has_select THEN
        RAISE NOTICE 'Granting SELECT on %.% to %', permissions.table_schema, table_name, role_name;
        EXECUTE format('GRANT SELECT ON TABLE %I.%I TO %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_insert THEN
        RAISE NOTICE 'Granting INSERT on %.% to %', permissions.table_schema, table_name, role_name;
        EXECUTE format('GRANT INSERT ON TABLE %I.%I TO %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_update THEN
        RAISE NOTICE 'Granting UPDATE on %.% to %', permissions.table_schema, table_name, role_name;
        EXECUTE format('GRANT UPDATE ON TABLE %I.%I TO %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_delete THEN
        RAISE NOTICE 'Granting DELETE on %.% to %', permissions.table_schema, table_name, role_name;
        EXECUTE format('GRANT DELETE ON TABLE %I.%I TO %I', permissions.table_schema, table_name, role_name);
    END IF;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION "MOLGENIS".revoke_table_permissions(
    permissions RECORD,
    role_name TEXT,
    table_name TEXT
)
    RETURNS VOID AS
$$
BEGIN
    IF permissions.has_admin OR permissions.has_select THEN
        RAISE NOTICE 'Revoking SELECT on %.% from %', permissions.table_schema, table_name, role_name;
        EXECUTE format('REVOKE SELECT ON TABLE %I.%I FROM %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_admin OR permissions.has_insert THEN
        RAISE NOTICE 'Revoking INSERT on %.% from %', permissions.table_schema, table_name, role_name;
        EXECUTE format('REVOKE INSERT ON TABLE %I.%I FROM %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_admin OR permissions.has_update THEN
        RAISE NOTICE 'Revoking UPDATE on %.% from %', permissions.table_schema, table_name, role_name;
        EXECUTE format('REVOKE UPDATE ON TABLE %I.%I FROM %I', permissions.table_schema, table_name, role_name);
    END IF;
    IF permissions.has_admin OR permissions.has_delete THEN
        RAISE NOTICE 'Revoking DELETE on %.% from %', permissions.table_schema, table_name, role_name;
        EXECUTE format('REVOKE DELETE ON TABLE %I.%I FROM %I', permissions.table_schema, table_name, role_name);
    END IF;
END;
$$ LANGUAGE plpgsql;


-- Function to create or update schema-level permissions for a schema
CREATE OR REPLACE FUNCTION "MOLGENIS".create_or_update_schema_groups(
    schema_id TEXT
)
    RETURNS void
    LANGUAGE plpgsql AS
$$
DECLARE
    group_name TEXT;
    role_name  TEXT;
BEGIN
    -- Ensure global admin group exists -- TODO do this somewhere else -> in migration script
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_ROLE_ADMIN') THEN
        INSERT INTO "MOLGENIS".group_metadata(group_name) VALUES ('ADMIN');
        INSERT INTO "MOLGENIS".group_permissions(group_name, table_schema, has_select, has_update, has_delete,
                                                 has_insert, has_admin)
        VALUES ('ADMIN', schema_id, true, true, true, true, true)
        ON CONFLICT DO NOTHING;
    END IF;

    EXECUTE format('GRANT ALL ON SCHEMA %I TO %I', schema_id, 'MG_ROLE_ADMIN');

    -- Create _ADMIN group for the schema
    group_name := format('%I_ADMIN', schema_id);
    role_name := format('MG_ROLE_%I', group_name);
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
        INSERT INTO "MOLGENIS".group_metadata(group_name, users)
        VALUES (group_name, ARRAY []::varchar[]);
        INSERT INTO "MOLGENIS".group_permissions(group_name, table_schema, has_update, has_insert, has_delete,
                                                 has_select, has_admin)
        VALUES (group_name, schema_id, true, true, true, true, true);
    END IF;

    -- Create _VIEWER group with select permissions
    group_name := format('%I_VIEWER', schema_id);
    role_name := format('MG_ROLE_%I', group_name);
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
        INSERT INTO "MOLGENIS".group_metadata(group_name, users)
        VALUES (group_name, ARRAY []::varchar[]);

        INSERT INTO "MOLGENIS".group_permissions(group_name, table_schema, has_select)
        VALUES (group_name, schema_id, true);
    END IF;

    -- Create _EDITOR group with select, insert, update, delete permissions
    group_name := format('%I_EDITOR', schema_id);
    role_name := format('MG_ROLE_%I', group_name);
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
        INSERT INTO "MOLGENIS".group_metadata(group_name, users)
        VALUES (group_name, ARRAY []::varchar[])
        ON CONFLICT DO NOTHING;

        INSERT INTO "MOLGENIS".group_permissions(group_name, table_schema, has_select, has_insert, has_update,
                                                 has_delete)
        VALUES (group_name, schema_id, true, true, true, true);
    END IF;

END;
$$;

-- Function to enable RLS
CREATE OR REPLACE FUNCTION "MOLGENIS".enable_RLS_on_table(schema_id TEXT, table_id TEXT)
    RETURNS void AS
$$
DECLARE
    policies_exists TEXT[];
    safe_schema_id  TEXT := replace(schema_id, ' ', '_'); -- todo: change this toCamelCase
    safe_table_id   TEXT := replace(table_id, ' ', '_');
BEGIN
    -- check if table exists
    IF NOT EXISTS (SELECT 1
                   FROM "MOLGENIS".table_metadata
                   WHERE table_schema = schema_id
                     AND table_name = table_id) THEN
        RAISE EXCEPTION 'Table %.% does not exist.', schema_id, table_id;
    END IF;

    -- Ensure the 'mg_group' column exists
    EXECUTE format(
            'ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS mg_group TEXT[] DEFAULT NULL',
            schema_id, table_id
            );

    EXECUTE format(
            'ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY',
            schema_id, table_id
            );

    -- Fetch existing policies
    SELECT COALESCE(array_agg(policyname), ARRAY []::TEXT[])
    INTO policies_exists
    FROM pg_policies
    WHERE schemaname = schema_id
      AND tablename = table_id;

    -- Create SELECT policy if not exists
    IF NOT ('select_policy_' || safe_schema_id || '_' || safe_table_id = ANY (policies_exists)) THEN
        EXECUTE format('CREATE POLICY select_policy_%s_%s ON %I.%I FOR SELECT USING (
                            EXISTS (
                                SELECT 1
                                FROM "MOLGENIS".user_permissions_mv u
                                WHERE u.user_name = current_user
                                  AND u.table_schema = %L
                                  AND (u.table_name = %L OR u.table_name IS NULL)
                                  AND u.has_select
                                  AND u.group_name = ANY(mg_group)
                            )
                        )',
                       safe_schema_id, safe_table_id, schema_id, table_id, schema_id, table_id
                );
        RAISE NOTICE 'Select policy created on %.%', schema_id, table_id;
    END IF;

    -- Create INSERT policy if not exists
    IF NOT ('insert_policy_' || safe_schema_id || '_' || safe_table_id = ANY (policies_exists)) THEN
        EXECUTE format(
                'CREATE POLICY insert_policy_%s_%s ON %I.%I FOR INSERT WITH CHECK (
                    EXISTS (
                        SELECT 1
                        FROM "MOLGENIS".user_permissions_mv u
                        WHERE u.user_name = current_user
                          AND u.table_schema = %L
                          AND (u.table_name = %L OR u.table_name IS NULL)
                          AND u.has_insert
                    )
                )',
                safe_schema_id, safe_table_id, schema_id, table_id, schema_id, table_id
                );
        RAISE NOTICE 'Insert policy created on %.%', schema_id, table_id;

    END IF;

    -- Create UPDATE policy if not exists
    IF NOT ('update_policy_' || safe_schema_id || '_' || safe_table_id = ANY (policies_exists)) THEN
        EXECUTE format(
                'CREATE POLICY update_policy_%s_%s ON %I.%I FOR UPDATE USING (
                    EXISTS (
                        SELECT 1
                        FROM "MOLGENIS".user_permissions_mv u
                        WHERE u.user_name = current_user
                          AND u.table_schema = %L
                          AND (u.table_name = %L OR u.table_name IS NULL)
                          AND u.has_update
                          AND u.group_name = ANY(mg_group)
                    )
                )',
                safe_schema_id, safe_table_id, schema_id, table_id, schema_id, table_id
                );
        RAISE NOTICE 'Update policy created on %.%', schema_id, table_id;
    END IF;

    -- Create DELETE policy if not exists
    IF NOT ('delete_policy_' || safe_schema_id || '_' || safe_table_id = ANY (policies_exists)) THEN
        EXECUTE format(
                'CREATE POLICY delete_policy_%s_%s ON %I.%I FOR DELETE USING (
                    EXISTS (
                        SELECT 1
                        FROM "MOLGENIS".user_permissions_mv u
                        WHERE u.user_name = current_user
                          AND u.table_schema = %L
                          AND (u.table_name = %L OR u.table_name IS NULL)
                          AND u.has_delete
                          AND u.group_name = ANY(mg_group)
                    )
                )',
                safe_schema_id, safe_table_id, schema_id, table_id, schema_id, table_id
                );
        RAISE NOTICE 'Delete policy created on %.%', schema_id, table_id;
    END IF;
END;
$$ LANGUAGE plpgsql;
