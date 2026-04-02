DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'MOLGENIS'
          AND table_name = 'table_metadata'
          AND column_name = 'table_inherits'
          AND data_type = 'character varying'
    ) THEN
        ALTER TABLE "MOLGENIS"."table_metadata"
            ALTER COLUMN "table_inherits" TYPE varchar[]
                USING CASE WHEN "table_inherits" IS NULL OR "table_inherits" = '' THEN NULL ELSE ARRAY["table_inherits"] END;
    END IF;
END $$;
