-- ========================================
-- Tables
-- ========================================
ALTER TABLE "MOLGENIS".table_metadata
    ADD COLUMN IF NOT EXISTS row_level_security BOOLEAN DEFAULT FALSE;

-- Create groups metadata table (passive storage, no triggers)
CREATE TABLE IF NOT EXISTS "MOLGENIS".group_metadata
(
    group_name        TEXT PRIMARY KEY, -- Friendly name for the group
    group_description TEXT,             -- Optional description of the group
    users             TEXT[]            -- List of users in the group
);
ALTER TABLE "MOLGENIS".group_metadata
    OWNER TO molgenis;
GRANT DELETE, INSERT, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON "MOLGENIS".group_metadata TO PUBLIC;

-- Create groups permissions table (passive storage, no triggers)
CREATE TABLE IF NOT EXISTS "MOLGENIS".group_permissions
(
    group_name   TEXT    NOT NULL,
    table_schema TEXT    NULL,
    table_name   TEXT    NULL,
    is_row_level BOOLEAN NOT NULL DEFAULT FALSE,
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

-- Materialized view for efficient RLS policy evaluation
CREATE MATERIALIZED VIEW IF NOT EXISTS "MOLGENIS".user_permissions_mv AS
SELECT gp.group_name,
       gp.table_schema,
       gp.table_name,
       gp.is_row_level,
       gp.has_select,
       gp.has_insert,
       gp.has_update,
       gp.has_delete,
       gp.has_admin,
       u.rolname AS user_name
FROM "MOLGENIS".group_permissions gp
         JOIN pg_roles g
              ON g.rolname = 'MG_ROLE_' || gp.group_name
         JOIN pg_auth_members am
              ON am.roleid = g.oid
         JOIN pg_roles u
              ON u.oid = am.member
                  AND u.rolname LIKE 'MG_USER_%'
WITH NO DATA;
ALTER MATERIALIZED VIEW "MOLGENIS".user_permissions_mv
    OWNER TO molgenis;
GRANT SELECT ON "MOLGENIS".user_permissions_mv TO PUBLIC;
REFRESH MATERIALIZED VIEW "MOLGENIS".user_permissions_mv;
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_schema
    ON "MOLGENIS".user_permissions_mv (user_name, table_schema, table_name);

-- ========================================
-- Schema metadata policy (uses the materialized view)
-- ========================================
DROP POLICY IF EXISTS "schema_metadata_POLICY" ON "MOLGENIS".schema_metadata;
CREATE POLICY "schema_metadata_POLICY"
    ON "MOLGENIS".schema_metadata
    AS PERMISSIVE
    FOR ALL
    USING (
    EXISTS (SELECT 1
            FROM "MOLGENIS".user_permissions_mv up
            WHERE up.table_schema = schema_metadata.table_schema
              AND up.user_name IN (current_user, 'MG_USER_anonymous'))
    );

-- ========================================
-- Refresh function (called from Java after permission changes)
-- ========================================
CREATE OR REPLACE FUNCTION "MOLGENIS".refresh_user_permissions_mv()
    RETURNS void
    LANGUAGE sql
    SECURITY DEFINER
AS
$$
REFRESH MATERIALIZED VIEW "MOLGENIS".user_permissions_mv;
$$;
GRANT EXECUTE ON FUNCTION "MOLGENIS".refresh_user_permissions_mv() TO PUBLIC;

-- ========================================
-- NOTE: All trigger functions, triggers, and helper PL/pgSQL functions
-- (group_metadata_trigger_function, group_permissions_trigger_function,
--  table_metadata_trigger_function, schema_metadata_trigger_function,
--  grant_table_permissions, revoke_table_permissions,
--  create_or_update_schema_groups, enable_RLS_on_table)
-- have been moved to SqlPermissionExecutor.java for better
-- discoverability, debuggability, and testability.
-- ========================================
