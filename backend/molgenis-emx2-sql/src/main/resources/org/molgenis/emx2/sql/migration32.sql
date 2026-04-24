-- migration 32: fine-grained permissions (populated incrementally)

CREATE TABLE IF NOT EXISTS "MOLGENIS"."role_metadata" (
    role_name VARCHAR NOT NULL,
    schema_name VARCHAR NOT NULL DEFAULT '*',
    description VARCHAR,
    immutable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR NOT NULL DEFAULT 'active' CHECK (status IN ('active','deleted')),
    created_by VARCHAR,
    created_on TIMESTAMPTZ,
    deleted_on TIMESTAMPTZ,
    PRIMARY KEY (role_name, schema_name)
);

ALTER TABLE "MOLGENIS"."table_metadata"
    ADD COLUMN IF NOT EXISTS row_level_security BOOLEAN NOT NULL DEFAULT FALSE;

CREATE OR REPLACE FUNCTION "MOLGENIS".current_user_roles()
RETURNS text[]
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        string_to_array(current_setting('molgenis.current_roles', true), ','),
        ARRAY(
            SELECT regexp_replace(rolname, '^MG_ROLE_', '')
            FROM pg_auth_members
            JOIN pg_roles ON oid = roleid
            WHERE member = (SELECT oid FROM pg_roles WHERE rolname = current_user)
        )
    )
$$;

ALTER ROLE "MG_USER_admin" BYPASSRLS;

CREATE TABLE IF NOT EXISTS "MOLGENIS"."permission_attributes" (
    role_name VARCHAR NOT NULL,
    schema_name VARCHAR NOT NULL,
    table_name VARCHAR NOT NULL,
    change_owner BOOLEAN NOT NULL DEFAULT FALSE,
    share BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (role_name, schema_name, table_name)
);

CREATE TABLE IF NOT EXISTS "MOLGENIS"."role_wildcards" (
    role_name VARCHAR NOT NULL,
    schema_pattern VARCHAR NOT NULL DEFAULT '*',
    table_pattern VARCHAR NOT NULL DEFAULT '*',
    select_scope VARCHAR,
    insert_scope VARCHAR,
    update_scope VARCHAR,
    delete_scope VARCHAR,
    change_owner BOOLEAN NOT NULL DEFAULT FALSE,
    share BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (role_name, schema_pattern, table_pattern)
);

-- mg_reserved_column_guard (authoritative copy at resources/sql/rls/mg_reserved_column_guard.sql)
CREATE OR REPLACE FUNCTION "MOLGENIS".mg_reserved_column_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    caller_roles text[];
    role_name_stripped text;
    allowed_change_owner boolean := false;
    allowed_share boolean := false;
BEGIN
    IF OLD.mg_owner IS NOT DISTINCT FROM NEW.mg_owner
       AND OLD.mg_roles IS NOT DISTINCT FROM NEW.mg_roles THEN
        RETURN NEW;
    END IF;

    caller_roles := "MOLGENIS".current_user_roles();

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_user AND rolbypassrls) THEN
        RETURN NEW;
    END IF;

    IF OLD.mg_owner IS DISTINCT FROM NEW.mg_owner THEN
        FOR role_name_stripped IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM "MOLGENIS"."permission_attributes"
                WHERE role_name = regexp_replace(role_name_stripped, '^MG_ROLE_', '')
                  AND schema_name = TG_TABLE_SCHEMA
                  AND table_name  = TG_TABLE_NAME
                  AND change_owner = true
            ) THEN
                allowed_change_owner := true;
                EXIT;
            END IF;
        END LOOP;
        IF NOT allowed_change_owner THEN
            RAISE EXCEPTION 'change_owner not permitted on %.% for current_user %', TG_TABLE_SCHEMA, TG_TABLE_NAME, current_user;
        END IF;
    END IF;

    IF OLD.mg_roles IS DISTINCT FROM NEW.mg_roles THEN
        FOR role_name_stripped IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM "MOLGENIS"."permission_attributes"
                WHERE role_name = regexp_replace(role_name_stripped, '^MG_ROLE_', '')
                  AND schema_name = TG_TABLE_SCHEMA
                  AND table_name  = TG_TABLE_NAME
                  AND share = true
            ) THEN
                allowed_share := true;
                EXIT;
            END IF;
        END LOOP;
        IF NOT allowed_share THEN
            RAISE EXCEPTION 'share not permitted on %.% for current_user %', TG_TABLE_SCHEMA, TG_TABLE_NAME, current_user;
        END IF;
        IF NOT (NEW.mg_roles <@ ARRAY(
            SELECT regexp_replace(unnest(caller_roles), '^MG_ROLE_', '')
        )) THEN
            RAISE EXCEPTION 'mg_roles contains role(s) the caller does not hold';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;
