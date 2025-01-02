ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN profile varchar;

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN profile_migration_step int;
