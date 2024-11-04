CREATE TABLE "MOLGENIS"."schema_version" (
    "version" VARCHAR NOT NULL,
    "schema" VARCHAR NOT NULL,
    PRIMARY KEY ("version", "schema")
);

ALTER TABLE "MOLGENIS"."schema_version"
    ADD COLUMN "previous_version" VARCHAR,
    ADD CONSTRAINT fk_previous_version
        FOREIGN KEY ("previous_version", "schema")
            REFERENCES "MOLGENIS"."schema_version" ("version", "schema")
            ON DELETE SET NULL;

GRANT SELECT, INSERT, UPDATE, DELETE ON "MOLGENIS"."schema_version" TO "molgenis";

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN version VARCHAR,
    ADD CONSTRAINT fk_versions
        FOREIGN KEY ("version", "table_schema")
            REFERENCES "MOLGENIS"."schema_version" ("version", "schema");

