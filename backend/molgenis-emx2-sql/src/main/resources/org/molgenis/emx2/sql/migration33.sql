ALTER TABLE "MOLGENIS".table_metadata ALTER COLUMN table_inherits TYPE VARCHAR[] USING (CASE WHEN table_inherits IS NULL THEN NULL ELSE ARRAY[table_inherits] END);
ALTER TABLE "MOLGENIS".column_metadata ADD COLUMN IF NOT EXISTS "values" VARCHAR[];
DO $$
    DECLARE rec RECORD;
    BEGIN
        FOR rec IN
            SELECT c.table_schema AS schema_name, c.table_name AS table_name
            FROM information_schema.columns c
            JOIN "MOLGENIS".table_metadata tm
              ON tm.table_schema = c.table_schema
             AND tm.table_name = c.table_name
            WHERE c.column_name = 'mg_tableclass'
              AND tm.table_inherits IS NOT NULL
        LOOP
            EXECUTE format('ALTER TABLE %I.%I DROP COLUMN IF EXISTS mg_tableclass CASCADE', rec.schema_name, rec.table_name);
        END LOOP;
    END $$;
