------------------------------------------------------------------------------
-- 1. Create migration schema + temp tables
------------------------------------------------------------------------------

CREATE SCHEMA IF NOT EXISTS temp_migration;

CREATE TABLE temp_migration.roles (
                                      role_name TEXT PRIMARY KEY,
                                      is_user_role BOOLEAN,
                                      mapped_user TEXT
);

CREATE TABLE temp_migration.group_metadata AS
SELECT *
FROM "MOLGENIS".group_metadata
WHERE 1=0;

CREATE TABLE temp_migration.group_permissions AS
SELECT *
FROM "MOLGENIS".group_permissions
WHERE 1=0;


------------------------------------------------------------------------------
-- 2. Extract MG_ROLE_* group roles (without prefix)
------------------------------------------------------------------------------

INSERT INTO temp_migration.roles (role_name, is_user_role)
SELECT rolname, FALSE
FROM pg_roles
WHERE rolname LIKE 'MG_ROLE_%';


------------------------------------------------------------------------------
-- 3. Extract MG_USER_* roles + mapped username
------------------------------------------------------------------------------

INSERT INTO temp_migration.roles (role_name, is_user_role, mapped_user)
SELECT rolname, TRUE, regexp_replace(rolname, '^MG_USER_', '')
FROM pg_roles
WHERE rolname LIKE 'MG_USER_%';


------------------------------------------------------------------------------
-- 4. Extract memberships (MG_ROLE_* contains MG_USER_*) with group_name cleaned
------------------------------------------------------------------------------

INSERT INTO temp_migration.group_metadata (group_name, users)
SELECT
    regexp_replace(r.rolname, '^MG_ROLE_', '') AS group_name,
    ARRAY(
            SELECT regexp_replace(u.rolname, '^MG_USER_', '')
            FROM pg_auth_members m
                     JOIN pg_roles u ON u.oid = m.member
            WHERE m.roleid = r.oid
              AND u.rolname LIKE 'MG_USER_%'
    ) AS users
FROM pg_roles r
WHERE r.rolname LIKE 'MG_ROLE_%';


------------------------------------------------------------------------------
-- 5. Extract schema-level permissions
--    CRUD derived from table grants (same for all tables in schema)
--    Admin derived from schema CREATE privilege
------------------------------------------------------------------------------

WITH table_privs AS (
    SELECT
        regexp_replace(grantee, '^MG_ROLE_', '') AS group_name,
        table_schema,
        MAX(CASE WHEN privilege_type = 'SELECT' THEN 1 ELSE 0 END) AS has_select,
        MAX(CASE WHEN privilege_type = 'INSERT' THEN 1 ELSE 0 END) AS has_insert,
        MAX(CASE WHEN privilege_type = 'UPDATE' THEN 1 ELSE 0 END) AS has_update,
        MAX(CASE WHEN privilege_type = 'DELETE' THEN 1 ELSE 0 END) AS has_delete
    FROM information_schema.role_table_grants
    WHERE grantee LIKE 'MG_ROLE_%'
    GROUP BY grantee, table_schema
),

     schema_admin AS (
         SELECT
             regexp_replace(r.rolname, '^MG_ROLE_', '') AS group_name,
             n.nspname AS table_schema,
             MAX(
                     CASE
                         WHEN acl_txt LIKE (r.rolname || '=%') AND acl_txt LIKE '%C%'
                             THEN 1 ELSE 0
                         END
             ) AS has_admin
         FROM pg_roles r
                  CROSS JOIN pg_namespace n
                  LEFT JOIN LATERAL (
             SELECT acl::text AS acl_txt
             FROM unnest(n.nspacl) AS acl
             ) a ON TRUE
         WHERE r.rolname LIKE 'MG_ROLE_%'
         GROUP BY r.rolname, n.nspname
     )

INSERT INTO temp_migration.group_permissions (
    group_name, table_schema, table_name,
    is_row_level,
    has_select, has_insert, has_update, has_delete,
    has_admin
)
SELECT
    tp.group_name,
    tp.table_schema,
    NULL AS table_name,
    FALSE AS is_row_level,
    (tp.has_select = 1),
    (tp.has_insert = 1),
    (tp.has_update = 1),
    (tp.has_delete = 1),
    (COALESCE(sa.has_admin, 0) = 1)
FROM table_privs tp
         LEFT JOIN schema_admin sa
                   ON sa.group_name = tp.group_name
                       AND sa.table_schema = tp.table_schema;


------------------------------------------------------------------------------
-- 6. Drop old Postgres roles safely
------------------------------------------------------------------------------

DO $$
    DECLARE r TEXT;
    BEGIN
        -- Reassign ownership and drop objects first
        FOR r IN SELECT role_name FROM temp_migration.roles ORDER BY role_name DESC
            LOOP
                EXECUTE format('REASSIGN OWNED BY %I TO molgenis;', r);
                EXECUTE format('DROP OWNED BY %I;', r);
            END LOOP;

        -- Drop roles
        FOR r IN SELECT role_name FROM temp_migration.roles ORDER BY role_name DESC
            LOOP
                EXECUTE format('DROP ROLE IF EXISTS %I;', r);
            END LOOP;
    END;
$$;


------------------------------------------------------------------------------
-- 7. Re-create groups via MOLGENIS triggers
------------------------------------------------------------------------------

INSERT INTO "MOLGENIS".group_metadata (group_name, users)
SELECT group_name, users
FROM temp_migration.group_metadata;


------------------------------------------------------------------------------
-- 8. Re-create permissions via MOLGENIS triggers
------------------------------------------------------------------------------

INSERT INTO "MOLGENIS".group_permissions (
    group_name, table_schema, table_name,
    is_row_level,
    has_select, has_insert, has_update, has_delete, has_admin
)
SELECT
    group_name, table_schema, table_name,
    is_row_level,
    has_select, has_insert, has_update, has_delete, has_admin
FROM temp_migration.group_permissions;


------------------------------------------------------------------------------
-- 9. Done
------------------------------------------------------------------------------

SELECT 'Migration completed successfully' AS status;
