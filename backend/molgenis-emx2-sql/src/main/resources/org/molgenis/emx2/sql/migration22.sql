DO $$
    DECLARE
        schema_exists BOOLEAN;
        table_exists BOOLEAN;
    BEGIN
        -- Check if _SYSTEM_ exists
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.schemata
            WHERE schema_name = '_SYSTEM_'
        ) INTO schema_exists;

        IF schema_exists THEN
            -- Check if the Scripts exists
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = '_SYSTEM_' AND table_name = 'Scripts'
            ) INTO table_exists;

            IF table_exists THEN
                EXECUTE 'ALTER TABLE "_SYSTEM_"."Scripts" ADD COLUMN IF NOT EXISTS failureAddress VARCHAR(255)';
                EXECUTE '
                INSERT INTO "MOLGENIS"."column_metadata" (
                    table_schema, table_name, column_name, "columnType", key, position, required,
                    ref_schema, ref_table, ref_link, "refLabel", "refBack", validation, computed,
                    indexed, cascade, description, "columnSemantics", visible, readonly, label,
                    "columnProfiles", "defaultValue"
                ) VALUES (
                    ''_SYSTEM_'', ''Scripts'', ''failureAddress'', ''EMAIL'', 0, 8, null, null, null, null, null, null, null, null, false,
                    false, ''{
                        "en": "Email address to be notified when a job fails"
                    }'', null, null, false, ''{}'', null, null
                )
                ON CONFLICT (table_schema, table_name, column_name)
                DO NOTHING;
            ';
            ELSE
                RAISE NOTICE 'Table "Scripts" does not exist in schema "_SYSTEM_"';
            END IF;
        ELSE
            RAISE NOTICE 'Schema "_SYSTEM_" does not exist';
        END IF;
    END $$;