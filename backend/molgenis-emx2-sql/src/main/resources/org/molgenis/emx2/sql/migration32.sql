DO $$
BEGIN
    -- convert table_inherits from varchar to varchar[] for multi-inheritance
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

    -- rename table_type BLOCK to INTERNAL
    UPDATE "MOLGENIS"."table_metadata"
    SET "table_type" = 'INTERNAL'
    WHERE "table_type" = 'BLOCK';

    -- rename column_type PROFILE/PROFILES directly to VARIANT/VARIANT_ARRAY
    UPDATE "MOLGENIS"."column_metadata" SET "columnType" = 'VARIANT' WHERE "columnType" IN ('PROFILE', 'EXTENSION');
    UPDATE "MOLGENIS"."column_metadata" SET "columnType" = 'VARIANT_ARRAY' WHERE "columnType" IN ('PROFILES', 'EXTENSION_ARRAY');

    -- add table and schema profiles columns
    ALTER TABLE "MOLGENIS"."table_metadata" ADD COLUMN IF NOT EXISTS "tableProfiles" varchar[];
    ALTER TABLE "MOLGENIS"."schema_metadata" ADD COLUMN IF NOT EXISTS "schemaProfiles" varchar[];

    -- rename table_inherits to table_extends (must happen after varchar[] conversion above)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'MOLGENIS'
          AND table_name = 'table_metadata'
          AND column_name = 'table_inherits'
    ) THEN
        ALTER TABLE "MOLGENIS"."table_metadata" RENAME COLUMN "table_inherits" TO "table_extends";
    END IF;
END $$;
