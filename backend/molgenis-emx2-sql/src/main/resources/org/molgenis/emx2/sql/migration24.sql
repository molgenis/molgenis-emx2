CREATE TYPE "MOLGENIS".app AS ENUM ('CATALOGUE', 'DIRECTORY');

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN IF NOT EXISTS app "MOLGENIS".app;

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN IF NOT EXISTS app_migration_version int;
