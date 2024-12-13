ALTER TABLE "MOLGENIS"."schema_metadata"
    DROP COLUMN profile;

ALTER TABLE "MOLGENIS"."schema_metadata"
    DROP COLUMN profile_migration_step;

DROP TYPE "MOLGENIS".profile;
