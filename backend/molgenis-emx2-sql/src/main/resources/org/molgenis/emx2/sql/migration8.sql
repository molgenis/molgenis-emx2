ALTER TABLE "MOLGENIS"."column_metadata"
    ADD COLUMN IF NOT EXISTS label JSON;

ALTER TABLE "MOLGENIS"."table_metadata"
    ADD COLUMN IF NOT EXISTS table_label JSON;

ALTER TABLE "MOLGENIS"."column_metadata"
    ALTER COLUMN description type JSON
    USING CASE WHEN description IS NOT NULL THEN json_build_object('en',description) END;

ALTER TABLE "MOLGENIS"."table_metadata"
ALTER COLUMN table_description type JSON
    USING CASE WHEN table_description IS NOT NULL THEN json_build_object('en',table_description) END;