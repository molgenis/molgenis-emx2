DO $migrate$
DECLARE
    changelog RECORD;
BEGIN
    FOR changelog IN
        SELECT n.nspname AS schema_name
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'mg_changelog' AND c.relkind = 'r'
    LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS mg_changelog_stamp_idx ON %I.mg_changelog (stamp)', changelog.schema_name);
    END LOOP;
END
$migrate$;
