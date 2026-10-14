CREATE OR REPLACE FUNCTION "MOLGENIS".mg_changelog_audit() RETURNS TRIGGER AS $audit$
DECLARE
    old_row jsonb := to_jsonb(OLD);
    new_row jsonb := to_jsonb(NEW);
    content_key text;
BEGIN
    IF (TG_OP = 'UPDATE' AND old_row - 'mg_updatedOn' - 'mg_updatedBy' = new_row - 'mg_updatedOn' - 'mg_updatedBy') THEN
        RETURN NULL;
    END IF;
    FOR content_key IN
        SELECT k FROM jsonb_object_keys(COALESCE(old_row, new_row)) AS k
        WHERE right(k, 9) = '_contents'
          AND jsonb_exists(COALESCE(old_row, new_row), left(k, -9) || '_mimetype')
    LOOP
        old_row := old_row - content_key;
        new_row := new_row - content_key;
    END LOOP;
    EXECUTE format('INSERT INTO %I.mg_changelog SELECT $1, now(), user, $2, $3, $4', TG_TABLE_SCHEMA)
        USING left(TG_OP, 1), TG_TABLE_NAME, CAST(old_row AS json), CAST(new_row AS json);
    RETURN NULL;
END;
$audit$ LANGUAGE plpgsql;

DO $migrate$
DECLARE
    changelog RECORD;
    audit RECORD;
BEGIN
    FOR changelog IN
        SELECT n.nspname AS schema_name
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'mg_changelog' AND c.relkind = 'r'
    LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS mg_changelog_stamp_idx ON %I.mg_changelog (stamp)', changelog.schema_name);
    END LOOP;

    FOR audit IN
        SELECT n.nspname AS schema_name, c.relname AS table_name, t.tgname AS trigger_name, p.oid::regprocedure AS old_function
        FROM pg_trigger t
        JOIN pg_class c ON c.oid = t.tgrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_proc p ON p.oid = t.tgfoid AND p.pronamespace = n.oid
        WHERE NOT t.tgisinternal
          AND p.proname LIKE 'process\_%\_audit'
          AND to_regclass(format('%I.mg_changelog', n.nspname)) IS NOT NULL
    LOOP
        EXECUTE format('DROP TRIGGER %I ON %I.%I', audit.trigger_name, audit.schema_name, audit.table_name);
        EXECUTE format('CREATE TRIGGER %I AFTER INSERT OR UPDATE OR DELETE ON %I.%I FOR EACH ROW EXECUTE FUNCTION "MOLGENIS".mg_changelog_audit()', audit.trigger_name, audit.schema_name, audit.table_name);
        EXECUTE format('DROP FUNCTION %s', audit.old_function);
    END LOOP;
END
$migrate$;
