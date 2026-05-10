CREATE TABLE IF NOT EXISTS "MOLGENIS".groups_metadata (
    schema TEXT NOT NULL,
    name TEXT NOT NULL,
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
        CHECK (delete_scope IN ('NONE','OWN','GROUP','ALL')),
    reference_scope TEXT NOT NULL DEFAULT 'NONE',
    CONSTRAINT role_permission_reference_scope_check
        CHECK (reference_scope IN ('NONE','ALL'))
);

DO $$
BEGIN
    ALTER TABLE "MOLGENIS".role_permission_metadata
        ADD COLUMN reference_scope TEXT NOT NULL DEFAULT 'NONE';
    ALTER TABLE "MOLGENIS".role_permission_metadata
        ADD CONSTRAINT role_permission_reference_scope_check
        CHECK (reference_scope IN ('NONE','ALL'));
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;

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
    group_name  TEXT,
    role_name   TEXT NOT NULL,
    granted_by  TEXT NOT NULL DEFAULT current_user,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (schema_name)
        REFERENCES "MOLGENIS".schema_metadata(table_schema) ON DELETE CASCADE,
    FOREIGN KEY (schema_name, group_name)
        REFERENCES "MOLGENIS".groups_metadata(schema, name) ON DELETE CASCADE,
    FOREIGN KEY (user_name)
        REFERENCES "MOLGENIS".users_metadata(username) ON DELETE CASCADE
);

DO $$
BEGIN
    ALTER TABLE "MOLGENIS".group_membership_metadata
        ALTER COLUMN group_name DROP NOT NULL;
EXCEPTION WHEN others THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE "MOLGENIS".group_membership_metadata
        ADD CONSTRAINT group_membership_schema_fk
        FOREIGN KEY (schema_name)
        REFERENCES "MOLGENIS".schema_metadata(table_schema) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE "MOLGENIS".group_membership_metadata
        DROP CONSTRAINT IF EXISTS group_membership_metadata_pkey;
EXCEPTION WHEN others THEN NULL;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS group_membership_uq_idx
    ON "MOLGENIS".group_membership_metadata (user_name, schema_name, group_name, role_name)
    NULLS NOT DISTINCT;

CREATE INDEX IF NOT EXISTS group_membership_user_schema_idx
    ON "MOLGENIS".group_membership_metadata (user_name, schema_name);

DROP FUNCTION IF EXISTS "MOLGENIS".current_user_groups(TEXT);

CREATE FUNCTION "MOLGENIS".current_user_groups(p_schema TEXT)
    RETURNS TEXT[] LANGUAGE sql STABLE AS $$
    SELECT COALESCE(array_agg(DISTINCT group_name), ARRAY[]::TEXT[])
    FROM "MOLGENIS".group_membership_metadata
    WHERE user_name = regexp_replace(current_user, '^MG_USER_', '')
      AND schema_name = p_schema
      AND group_name IS NOT NULL
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_read(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT EXISTS (
        SELECT 1
        WHERE pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
           OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
           OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Editor',  'MEMBER')
           OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Viewer',  'MEMBER')
        UNION ALL
        SELECT 1
        FROM "MOLGENIS".group_membership_metadata m
        JOIN "MOLGENIS".role_permission_metadata rp
          ON rp.schema_name = m.schema_name
         AND rp.role_name   = m.role_name
         AND rp.table_name  = p_table
        WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
          AND m.schema_name = p_schema
          AND (
               rp.select_scope = 'ALL'
            OR (rp.select_scope = 'GROUP' AND m.group_name = ANY(p_groups))
            OR (rp.select_scope = 'OWN'   AND 'MG_USER_' || p_owner = current_user)
            OR rp.select_scope IN ('EXISTS','COUNT','RANGE','AGGREGATE')
            OR rp.change_owner = true
          )
    )
$$;

DROP FUNCTION IF EXISTS "MOLGENIS".mg_can_reference(TEXT, TEXT, TEXT[], TEXT);

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_reference(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT,
    p_user TEXT DEFAULT current_user
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT EXISTS (
        SELECT 1
        WHERE pg_has_role(p_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
           OR pg_has_role(p_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
           OR pg_has_role(p_user, 'MG_ROLE_' || p_schema || '/Editor',  'MEMBER')
           OR pg_has_role(p_user, 'MG_ROLE_' || p_schema || '/Viewer',  'MEMBER')
        UNION ALL
        SELECT 1
        FROM "MOLGENIS".group_membership_metadata m
        JOIN "MOLGENIS".role_permission_metadata rp
          ON rp.schema_name = m.schema_name
         AND rp.role_name   = m.role_name
         AND rp.table_name  = p_table
        WHERE m.user_name = regexp_replace(p_user, '^MG_USER_', '')
          AND m.schema_name = p_schema
          AND (
               rp.reference_scope = 'ALL'
            OR rp.select_scope = 'ALL'
            OR (rp.select_scope = 'GROUP' AND m.group_name = ANY(p_groups))
            OR (rp.select_scope = 'OWN'   AND 'MG_USER_' || p_owner = p_user)
          )
    )
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_write(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT, p_verb TEXT
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT EXISTS (
        SELECT 1
        WHERE (p_verb = 'insert' OR p_verb = 'update' OR p_verb = 'delete')
          AND (
            pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
         OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
         OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Editor',  'MEMBER')
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
          AND CASE p_verb
                WHEN 'insert' THEN
                  rp.insert_scope = 'ALL'
                  OR (rp.insert_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.insert_scope = 'OWN'   AND 'MG_USER_' || p_owner = current_user)
                WHEN 'update' THEN
                  rp.update_scope = 'ALL'
                  OR (rp.update_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.update_scope = 'OWN'   AND 'MG_USER_' || p_owner = current_user)
                ELSE
                  rp.delete_scope = 'ALL'
                  OR (rp.delete_scope = 'GROUP' AND m.group_name = ANY(p_groups))
                  OR (rp.delete_scope = 'OWN'   AND 'MG_USER_' || p_owner = current_user)
              END
    )
$$;

CREATE OR REPLACE FUNCTION "MOLGENIS".mg_can_write_all(
    p_schema TEXT, p_table TEXT, p_groups TEXT[], p_owner TEXT, p_verb TEXT,
    p_changing_owner BOOLEAN, p_changing_group BOOLEAN
) RETURNS BOOLEAN LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT
        (
            pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
         OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
         OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Editor',  'MEMBER')
         OR EXISTS (
              SELECT 1
              FROM "MOLGENIS".role_permission_metadata rp
              JOIN pg_roles pgr ON pgr.rolname = 'MG_ROLE_' || p_schema || '/' || rp.role_name
              JOIN pg_auth_members pam ON pam.roleid = pgr.oid
              JOIN pg_roles usr ON usr.oid = pam.member AND usr.rolname = current_user
              WHERE rp.schema_name = p_schema
                AND rp.table_name  = p_table
                AND CASE p_verb
                      WHEN 'insert' THEN rp.insert_scope IN ('ALL','OWN')
                      WHEN 'update' THEN rp.update_scope IN ('ALL','OWN')
                      ELSE              rp.delete_scope IN ('ALL','OWN')
                    END
            )
         OR (
            array_length(p_groups, 1) IS NULL OR array_length(p_groups, 1) = 0
            OR p_groups <@ ARRAY(
                 SELECT DISTINCT m.group_name
                 FROM "MOLGENIS".group_membership_metadata m
                 JOIN "MOLGENIS".role_permission_metadata rp
                   ON rp.schema_name = m.schema_name
                  AND rp.role_name   = m.role_name
                  AND rp.table_name  = p_table
                 WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                   AND m.schema_name = p_schema
                   AND m.group_name IS NOT NULL
                   AND CASE p_verb
                         WHEN 'insert' THEN rp.insert_scope IN ('ALL','GROUP')
                         WHEN 'update' THEN rp.update_scope IN ('ALL','GROUP')
                         ELSE              rp.delete_scope IN ('ALL','GROUP')
                       END
               )
           )
        )
        AND (NOT p_changing_owner OR
              pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
           OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
           OR EXISTS (
              SELECT 1
              FROM "MOLGENIS".role_permission_metadata rp
              JOIN pg_roles pgr ON pgr.rolname = 'MG_ROLE_' || p_schema || '/' || rp.role_name
              JOIN pg_auth_members pam ON pam.roleid = pgr.oid
              JOIN pg_roles usr ON usr.oid = pam.member AND usr.rolname = current_user
              WHERE rp.schema_name = p_schema
                AND rp.table_name  = p_table
                AND rp.change_owner = true
            )
           OR EXISTS (
              SELECT 1
              FROM "MOLGENIS".group_membership_metadata m
              JOIN "MOLGENIS".role_permission_metadata rp
                ON rp.schema_name = m.schema_name
               AND rp.role_name   = m.role_name
               AND rp.table_name  = p_table
              WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                AND m.schema_name = p_schema
                AND rp.change_owner = true
            ))
        AND (NOT p_changing_group OR
              pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Owner',   'MEMBER')
           OR pg_has_role(current_user, 'MG_ROLE_' || p_schema || '/Manager', 'MEMBER')
           OR EXISTS (
              SELECT 1
              FROM "MOLGENIS".role_permission_metadata rp
              JOIN pg_roles pgr ON pgr.rolname = 'MG_ROLE_' || p_schema || '/' || rp.role_name
              JOIN pg_auth_members pam ON pam.roleid = pgr.oid
              JOIN pg_roles usr ON usr.oid = pam.member AND usr.rolname = current_user
              WHERE rp.schema_name = p_schema
                AND rp.table_name  = p_table
                AND rp.change_group = true
            )
           OR EXISTS (
              SELECT 1
              FROM "MOLGENIS".group_membership_metadata m
              JOIN "MOLGENIS".role_permission_metadata rp
                ON rp.schema_name = m.schema_name
               AND rp.role_name   = m.role_name
               AND rp.table_name  = p_table
              WHERE m.user_name = regexp_replace(current_user, '^MG_USER_', '')
                AND m.schema_name = p_schema
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
                             AND 'MG_USER_' || NEW.mg_owner IS DISTINCT FROM current_user);
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

ALTER TABLE IF EXISTS "MOLGENIS".table_metadata
    ADD COLUMN IF NOT EXISTS rls_enabled BOOLEAN NOT NULL DEFAULT false;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_schema, c.table_name
        FROM information_schema.columns c
        WHERE c.column_name = 'mg_owner'
          AND c.table_schema <> 'MOLGENIS'
          AND c.table_schema NOT LIKE 'pg_%'
          AND c.table_schema <> 'information_schema'
    LOOP
        EXECUTE format(
            'UPDATE %I.%I SET mg_owner = NULL '
            'WHERE mg_owner IS NOT NULL '
            'AND mg_owner NOT IN (SELECT username FROM "MOLGENIS".users_metadata)',
            r.table_schema, r.table_name);
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_schema, c.table_name
        FROM information_schema.columns c
        WHERE c.column_name = 'mg_owner'
          AND c.table_schema <> 'MOLGENIS'
          AND c.table_schema NOT LIKE 'pg_%'
          AND c.table_schema <> 'information_schema'
    LOOP
        BEGIN
            EXECUTE format(
                'ALTER TABLE %I.%I ADD CONSTRAINT %I FOREIGN KEY (mg_owner) '
                'REFERENCES "MOLGENIS".users_metadata(username) '
                'ON DELETE SET NULL ON UPDATE CASCADE',
                r.table_schema, r.table_name, r.table_name || '_mg_owner_fk');
        EXCEPTION
            WHEN duplicate_object THEN NULL;
        END;
    END LOOP;
END $$;
