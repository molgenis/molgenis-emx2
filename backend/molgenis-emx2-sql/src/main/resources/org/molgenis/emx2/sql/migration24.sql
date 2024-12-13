CREATE TYPE "MOLGENIS".profile AS ENUM ('DATA_CATALOGUE', 'DCAT');

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN IF NOT EXISTS profile "MOLGENIS".profile;

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN IF NOT EXISTS profile_migration_step int;
