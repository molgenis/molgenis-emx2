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
