ALTER TABLE "MOLGENIS"."table_metadata"
    ALTER COLUMN "table_inherits" TYPE jsonb
USING
        CASE
            WHEN "table_inherits" IS NULL OR "table_inherits" = ''  THEN NULL
            ELSE  jsonb_build_array(jsonb_build_object('tableName', "table_inherits"))
        END;