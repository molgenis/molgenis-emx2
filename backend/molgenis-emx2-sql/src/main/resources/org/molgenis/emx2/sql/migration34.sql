CREATE TABLE IF NOT EXISTS "MOLGENIS".role_permission_metadata (
    schema_name  TEXT NOT NULL,
    role_name    TEXT NOT NULL,
    table_name   TEXT NOT NULL,
    select_scope TEXT NOT NULL DEFAULT 'NONE',
    insert_scope TEXT NOT NULL DEFAULT 'NONE',
    update_scope TEXT NOT NULL DEFAULT 'NONE',
    delete_scope TEXT NOT NULL DEFAULT 'NONE',
    change_owner BOOLEAN NOT NULL DEFAULT FALSE,
    change_group BOOLEAN NOT NULL DEFAULT FALSE,
    description  TEXT,
    updated_by   TEXT NOT NULL DEFAULT current_user,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (schema_name, role_name, table_name),
    FOREIGN KEY (schema_name)
        REFERENCES "MOLGENIS".schema_metadata(table_schema) ON DELETE CASCADE,
    CONSTRAINT role_permission_select_scope_check
        CHECK (select_scope IN ('NONE','EXISTS','COUNT','RANGE','AGGREGATE','OWN','GROUP','ALL')),
    CONSTRAINT role_permission_insert_scope_check
        CHECK (insert_scope IN ('NONE','OWN','GROUP','ALL')),
    CONSTRAINT role_permission_update_scope_check
        CHECK (update_scope IN ('NONE','OWN','GROUP','ALL')),
    CONSTRAINT role_permission_delete_scope_check
        CHECK (delete_scope IN ('NONE','OWN','GROUP','ALL'))
);

CREATE INDEX IF NOT EXISTS role_permission_schema_table_idx
    ON "MOLGENIS".role_permission_metadata (schema_name, table_name);

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_protect_system_roles()
    RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.role_name IN ('Owner','Manager','Editor','Viewer')
        AND current_user <> 'admin'
        AND TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'System role rows are immutable' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' THEN RETURN NEW; END IF;
    RETURN OLD;
END; $$;

CREATE OR REPLACE TRIGGER mg_protect_system_roles
    BEFORE UPDATE OR DELETE ON "MOLGENIS".role_permission_metadata
    FOR EACH ROW EXECUTE FUNCTION "MOLGENIS".mg_protect_system_roles();

CREATE TABLE IF NOT EXISTS "MOLGENIS".group_membership_metadata (
    user_name   TEXT NOT NULL,
    schema_name TEXT NOT NULL,
    group_name  TEXT NOT NULL,
    role_name   TEXT NOT NULL,
    granted_by  TEXT NOT NULL DEFAULT current_user,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_name, schema_name, group_name, role_name),
    FOREIGN KEY (schema_name, group_name)
        REFERENCES "MOLGENIS".groups_metadata(schema, name) ON DELETE CASCADE,
    FOREIGN KEY (user_name)
        REFERENCES "MOLGENIS".users_metadata(username) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS group_membership_user_schema_idx
    ON "MOLGENIS".group_membership_metadata (user_name, schema_name);
