CREATE SCHEMA IF NOT EXISTS temp_migration;

CREATE TABLE temp_migration.roles (
                                      role_name TEXT PRIMARY KEY,
                                      is_user_role BOOLEAN,
                                      mapped_user TEXT
);

CREATE TABLE temp_migration.group_permissions AS
SELECT *
FROM "MOLGENIS".group_permissions
WHERE 1=0;

CREATE TABLE temp_migration.group_metadata AS
SELECT *
FROM "MOLGENIS".group_metadata
WHERE 1=0;

INSERT INTO temp_migration.roles (role_name, is_user_role)
SELECT rolname, FALSE
FROM pg_roles
WHERE rolname LIKE 'MG_ROLE_%';

INSERT INTO temp_migration.roles (role_name, is_user_role, mapped_user)
SELECT rolname, TRUE, regexp_replace(rolname, '^MG_USER_', '')
FROM pg_roles
WHERE rolname LIKE 'MG_USER_%';

INSERT INTO temp_migration.group_metadata (group_name, users)
SELECT
    r.rolname AS group_name,
    ARRAY(
            SELECT regexp_replace(u.rolname, '^MG_USER_', '')
            FROM pg_auth_members m
                     JOIN pg_roles u ON u.oid = m.member
            WHERE m.roleid = r.oid
              AND u.rolname LIKE 'MG_USER_%'
    ) AS users
FROM pg_roles r
WHERE r.rolname LIKE 'MG_ROLE_%';

INSERT INTO temp_migration.group_permissions (
    group_name, table_schema, table_name,
    has_select, has_insert, has_update, has_delete, has_admin
)
SELECT
    grantee AS group_name,
    table_schema,
    table_name,
    (MAX(CASE WHEN privilege_type='SELECT' THEN 1 ELSE 0 END) = 1) AS has_select,
    (MAX(CASE WHEN privilege_type='INSERT' THEN 1 ELSE 0 END) = 1) AS has_insert,
    (MAX(CASE WHEN privilege_type='UPDATE' THEN 1 ELSE 0 END) = 1) AS has_update,
    (MAX(CASE WHEN privilege_type='DELETE' THEN 1 ELSE 0 END) = 1) AS has_delete,
    FALSE AS has_admin
FROM information_schema.role_table_grants
WHERE grantee LIKE 'MG_ROLE_%'
GROUP BY grantee, table_schema, table_name;

/** Rollen droppen */
DO $$
    DECLARE r TEXT;
    BEGIN
        -- Drop memberships
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

/** Rollen inserten in nieuwe tabllen */
INSERT INTO "MOLGENIS".group_metadata (group_name, users)
SELECT group_name, users
FROM temp_migration.group_metadata;

INSERT INTO "MOLGENIS".group_permissions (
    group_name, table_schema, table_name,
    has_select, has_insert, has_update, has_delete, has_admin
)
SELECT
    group_name, table_schema, table_name,
    has_select, has_insert, has_update, has_delete, has_admin
FROM temp_migration.group_permissions;