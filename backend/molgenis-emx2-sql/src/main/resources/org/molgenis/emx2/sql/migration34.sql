DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM "MOLGENIS".table_metadata
                   WHERE table_schema = '_SYSTEM_'
                     AND table_name = 'Templates') THEN

            ALTER TABLE "_SYSTEM_"."Templates"
                ADD COLUMN IF NOT EXISTS "tableName" character varying;

            INSERT INTO "MOLGENIS".column_metadata (table_schema, table_name, column_name, "columnType",
                                                    key, position, cascade, indexed, readonly)
            VALUES ('_SYSTEM_', 'Templates', 'tableName', 'STRING', 0, 3, false, false, false)
            ON CONFLICT (table_schema, table_name, column_name) DO NOTHING;

            IF NOT EXISTS (SELECT 1
                           FROM pg_constraint
                           WHERE conname = 'Templates_tableName_fkey'
                             AND conrelid = '"_SYSTEM_"."Templates"'::regclass) THEN
                ALTER TABLE "_SYSTEM_"."Templates"
                    ADD CONSTRAINT "Templates_tableName_fkey"
                        FOREIGN KEY ("schema", "tableName")
                            REFERENCES "MOLGENIS"."table_metadata" ("table_schema", "table_name")
                            ON UPDATE CASCADE ON DELETE CASCADE;
            END IF;
        END IF;
    END
$$;
