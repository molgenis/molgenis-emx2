CREATE TABLE IF NOT EXISTS "MOLGENIS".groups_metadata (
    schema TEXT NOT NULL,
    name TEXT NOT NULL,
    users TEXT[],
    PRIMARY KEY (schema, name),
    FOREIGN KEY (schema) REFERENCES "MOLGENIS".schema_metadata(table_schema)
        ON UPDATE CASCADE ON DELETE CASCADE
);

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

DROP FUNCTION IF EXISTS "MOLGENIS".current_user_groups(TEXT);

CREATE FUNCTION "MOLGENIS".current_user_groups(p_schema TEXT)
    RETURNS TEXT[] LANGUAGE sql STABLE AS $$
    SELECT COALESCE(array_agg(DISTINCT group_name), ARRAY[]::TEXT[])
    FROM "MOLGENIS".group_membership_metadata
    WHERE user_name = regexp_replace(current_user, '^MG_USER_', '')
      AND schema_name = p_schema
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_read(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT EXISTS (
        SELECT 1 FROM "MOLGENIS".role_permission_metadata rp
        WHERE rp.schema_name = p_schema
          AND rp.table_name  = '*'
          AND rp.role_name   IN ('Owner','Manager','Editor','Viewer')
          AND pg_has_role(current_user,
                'MG_ROLE_' || p_schema || '/' || rp.role_name, 'MEMBER')
          AND (
               rp.select_scope = 'ALL'
            OR (rp.select_scope = 'GROUP' AND p_groups && "MOLGENIS".current_user_groups(p_schema))
            OR (rp.select_scope = 'OWN'   AND p_owner = current_user)
          )
        UNION ALL
        SELECT 1
        FROM "MOLGENIS".group_membership_metadata m
        JOIN "MOLGENIS".role_permission_metadata rp
          ON rp.schema_name = m.schema_name
         AND rp.role_name   = m.role_name
         AND rp.table_name  = p_table
        WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
          AND m.schema_name = p_schema
          AND rp.role_name  NOT IN ('Owner','Manager','Editor','Viewer')
          AND (
               rp.select_scope = 'ALL'
            OR (rp.select_scope = 'GROUP' AND m.group_name = ANY(p_groups))
            OR (rp.select_scope = 'OWN'   AND p_owner = current_user)
            OR rp.select_scope IN ('EXISTS','COUNT','RANGE','AGGREGATE')
            OR rp.change_owner = true
          )
    )
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_write(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT, p_verb TEXT
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT EXISTS (
        SELECT 1 FROM "MOLGENIS".role_permission_metadata rp
        WHERE rp.schema_name = p_schema
          AND rp.table_name  = '*'
          AND rp.role_name   IN ('Owner','Manager','Editor','Viewer')
          AND pg_has_role(current_user,
                'MG_ROLE_' || p_schema || '/' || rp.role_name, 'MEMBER')
          AND CASE p_verb
                WHEN 'insert' THEN rp.insert_scope = 'ALL'
                WHEN 'update' THEN rp.update_scope = 'ALL'
                ELSE               rp.delete_scope = 'ALL'
              END
        UNION ALL
        SELECT 1
        FROM "MOLGENIS".group_membership_metadata m
        JOIN "MOLGENIS".role_permission_metadata rp
          ON rp.schema_name = m.schema_name
         AND rp.role_name   = m.role_name
         AND rp.table_name  = p_table
        WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
          AND m.schema_name = p_schema
          AND rp.role_name  NOT IN ('Owner','Manager','Editor','Viewer')
          AND CASE p_verb
                WHEN 'insert' THEN
                  rp.insert_scope = 'ALL'
                  OR (rp.insert_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.insert_scope = 'OWN'   AND p_owner = current_user)
                WHEN 'update' THEN
                  rp.update_scope = 'ALL'
                  OR (rp.update_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.update_scope = 'OWN'   AND p_owner = current_user)
                ELSE
                  rp.delete_scope = 'ALL'
                  OR (rp.delete_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.delete_scope = 'OWN'   AND p_owner = current_user)
              END
    )
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_write_all(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT, p_verb TEXT,
    p_changing_owner BOOLEAN, p_changing_group BOOLEAN
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT
        (array_length(p_groups, 1) IS NULL OR array_length(p_groups, 1) = 0
         OR p_groups <@ ARRAY(
              SELECT DISTINCT m.group_name
              FROM "MOLGENIS".group_membership_metadata m
              JOIN "MOLGENIS".role_permission_metadata rp
                ON rp.schema_name = m.schema_name
               AND rp.role_name   = m.role_name
               AND rp.table_name  = p_table
              WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                AND m.schema_name = p_schema
                AND rp.role_name  NOT IN ('Owner','Manager','Editor','Viewer')
                AND CASE p_verb
                      WHEN 'insert' THEN rp.insert_scope IN ('ALL','GROUP')
                      WHEN 'update' THEN rp.update_scope IN ('ALL','GROUP')
                      ELSE              rp.delete_scope IN ('ALL','GROUP')
                    END
            )
        )
        AND (NOT p_changing_owner OR EXISTS (
              SELECT 1 FROM "MOLGENIS".role_permission_metadata rp
              WHERE rp.schema_name = p_schema AND rp.table_name = '*'
                AND rp.role_name IN ('Owner','Manager','Editor','Viewer')
                AND pg_has_role(current_user,
                      'MG_ROLE_' || p_schema || '/' || rp.role_name, 'MEMBER')
                AND rp.change_owner = true
              UNION ALL
              SELECT 1
              FROM "MOLGENIS".group_membership_metadata m
              JOIN "MOLGENIS".role_permission_metadata rp
                ON rp.schema_name = m.schema_name
               AND rp.role_name   = m.role_name
               AND rp.table_name  = p_table
              WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                AND m.schema_name = p_schema
                AND rp.role_name  NOT IN ('Owner','Manager','Editor','Viewer')
                AND rp.change_owner = true
            ))
        AND (NOT p_changing_group OR EXISTS (
              SELECT 1 FROM "MOLGENIS".role_permission_metadata rp
              WHERE rp.schema_name = p_schema AND rp.table_name = '*'
                AND rp.role_name IN ('Owner','Manager','Editor','Viewer')
                AND pg_has_role(current_user,
                      'MG_ROLE_' || p_schema || '/' || rp.role_name, 'MEMBER')
                AND rp.change_group = true
              UNION ALL
              SELECT 1
              FROM "MOLGENIS".group_membership_metadata m
              JOIN "MOLGENIS".role_permission_metadata rp
                ON rp.schema_name = m.schema_name
               AND rp.role_name   = m.role_name
               AND rp.table_name  = p_table
              WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                AND m.schema_name = p_schema
                AND rp.role_name  NOT IN ('Owner','Manager','Editor','Viewer')
                AND rp.change_group = true
            ))
$$;

DROP FUNCTION IF EXISTS "MOLGENIS".mg_privacy_count(TEXT, TEXT, TEXT);

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_privacy_count(p_count BIGINT)
    RETURNS BIGINT LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT GREATEST(CEIL(p_count::numeric / 10)::BIGINT * 10, 10::BIGINT)
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_check_change_capability()
    RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    p_schema TEXT := TG_ARGV[0];
    p_table  TEXT := TG_ARGV[1];
    p_changing_owner BOOLEAN;
    p_changing_group BOOLEAN;
    p_verb TEXT;
BEGIN
    IF pg_has_role(current_user,
           'MG_ROLE_' || p_schema || '/Manager', 'MEMBER') THEN
        RETURN NEW;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        p_verb           := 'update';
        p_changing_owner := (OLD.mg_owner IS DISTINCT FROM NEW.mg_owner);
        p_changing_group := (OLD.mg_groups IS DISTINCT FROM NEW.mg_groups);
    ELSE
        p_verb           := 'insert';
        p_changing_owner := (NEW.mg_owner IS NOT NULL
                             AND NEW.mg_owner IS DISTINCT FROM current_user);
        p_changing_group := FALSE;
    END IF;
    IF NOT "MOLGENIS".mg_can_write_all(
        p_schema, p_table, NEW.mg_groups, NEW.mg_owner,
        p_verb, p_changing_owner, p_changing_group) THEN
        RAISE EXCEPTION 'Permission denied: change_owner / change_group capability missing'
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END; $$;

DO $$
DECLARE
    s RECORD;
BEGIN
    FOR s IN SELECT table_schema FROM "MOLGENIS".schema_metadata LOOP
        INSERT INTO "MOLGENIS".role_permission_metadata
            (schema_name, role_name, table_name, select_scope, insert_scope, update_scope, delete_scope, change_owner, change_group)
        VALUES
            (s.table_schema, 'Owner',   '*', 'ALL', 'ALL',  'ALL',  'ALL',  TRUE,  TRUE),
            (s.table_schema, 'Manager', '*', 'ALL', 'ALL',  'ALL',  'ALL',  TRUE,  TRUE),
            (s.table_schema, 'Editor',  '*', 'ALL', 'ALL',  'ALL',  'ALL',  FALSE, FALSE),
            (s.table_schema, 'Viewer',  '*', 'ALL', 'NONE', 'NONE', 'NONE', FALSE, FALSE)
        ON CONFLICT DO NOTHING;

        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_ROLE_' || s.table_schema || '_MEMBER') THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN NOBYPASSRLS NOINHERIT', 'MG_ROLE_' || s.table_schema || '_MEMBER');
        END IF;
        EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I', s.table_schema, 'MG_ROLE_' || s.table_schema || '_MEMBER');
        EXECUTE format('GRANT %I TO %I', 'MG_ROLE_' || s.table_schema || '/Exists', 'MG_ROLE_' || s.table_schema || '_MEMBER');
        EXECUTE format('GRANT %I TO %I WITH ADMIN OPTION', 'MG_ROLE_' || s.table_schema || '_MEMBER', 'MG_ROLE_' || s.table_schema || '/Manager');
        EXECUTE format('GRANT %I TO %I WITH ADMIN OPTION', 'MG_ROLE_' || s.table_schema || '_MEMBER', 'MG_ROLE_' || s.table_schema || '/Owner');
    END LOOP;
END $$;
