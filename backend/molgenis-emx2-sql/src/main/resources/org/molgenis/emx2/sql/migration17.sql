DO
$$
DECLARE
    aTable RECORD;
    key_list VARCHAR[];
    alterStatement TEXT;
BEGIN
--     -- fix that the key=1 should be a primary key, not a unique (should speed up joins)
--     FOR aTable IN select * from "MOLGENIS"."table_metadata" LOOP
--         BEGIN
--             -- get the primary key for this table (emx2 names these constraints using %_KEY1)
--             SELECT ARRAY(SELECT DISTINCT column_name FROM information_schema.key_column_usage WHERE constraint_name LIKE '%_KEY1' AND table_name = aTable.table_name AND table_schema = aTable.table_schema) INTO key_list;
--             -- EXECUTE 'ALTER TABLE "' || aTable.table_schema || '"."' || aTable.table_name || '"  DROP CONSTRAINT IF EXISTS "' || aTable.table_name || '_KEY1"';
--             EXECUTE 'ALTER TABLE "' || aTable.table_schema || '"."' || aTable.table_name || '" ADD CONSTRAINT "'||aTable.table_name || '_KEY1" PRIMARY KEY ("' || ARRAY_TO_STRING(key_list,'","') ||'")';
--         END;
--     END LOOP;
    -- get all inherited tables so we can fix that their keys should refer to parent foreign key
    FOR aTable IN select * from "MOLGENIS"."table_metadata" where table_inherits IS NOT NULL LOOP
        BEGIN
            -- get the primary key for this table (emx2 names these constraints using %_KEY1)
            SELECT ARRAY(SELECT DISTINCT column_name FROM information_schema.key_column_usage WHERE constraint_name LIKE '%_KEY1' AND table_name = aTable.table_name AND table_schema = aTable.table_schema) INTO key_list;
            -- create the alter statement
            alterStatement := 'ALTER TABLE "' || aTable.table_schema || '"."' || aTable.table_name || '" ADD CONSTRAINT "fkey_'||aTable.table_name || '_extends_' || aTable.table_inherits || '" FOREIGN KEY ("' || ARRAY_TO_STRING(key_list,'","') ||'") REFERENCES "'|| COALESCE(aTable.import_schema,aTable.table_schema)|| '"."' || aTable.table_inherits || '"("' || ARRAY_TO_STRING(key_list,'","') ||'")';
            -- RAISE NOTICE 'sql: %', alterStatement;
            -- first drop in case in already existed (typically only during tests)
            EXECUTE 'ALTER TABLE "' || aTable.table_schema || '"."' || aTable.table_name || '"  DROP CONSTRAINT IF EXISTS "fkey_'||aTable.table_name || '_extends_' || aTable.table_inherits || '"';
            -- execute the alter statement
            EXECUTE alterStatement;
        END;
    END LOOP;
    COMMIT;
END;
$$
LANGUAGE plpgsql;