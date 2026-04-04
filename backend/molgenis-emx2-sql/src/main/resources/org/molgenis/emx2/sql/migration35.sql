DO $$ BEGIN
  ALTER TABLE "MOLGENIS"."table_metadata" ADD COLUMN IF NOT EXISTS "tableProfiles" varchar[];
  ALTER TABLE "MOLGENIS"."schema_metadata" ADD COLUMN IF NOT EXISTS "schemaProfiles" varchar[];
END $$;
