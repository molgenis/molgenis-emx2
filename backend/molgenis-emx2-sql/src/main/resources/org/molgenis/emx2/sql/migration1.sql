TRUNCATE "MOLGENIS"."version_metadata";
ALTER TABLE "MOLGENIS"."version_metadata" DROP COLUMN "version";
ALTER TABLE "MOLGENIS"."version_metadata"
    ADD COLUMN "version" INTEGER NOT NULL;