CREATE TABLE "MOLGENIS"."app_version" (
    "app" VARCHAR NOT NULL,
    "version" VARCHAR NOT NULL,
    "previous_version" VARCHAR,
    PRIMARY KEY ("app", "version"),
    CONSTRAINT fk_previous_version
        FOREIGN KEY (app, previous_version)
            REFERENCES "MOLGENIS"."app_version" (app, version),
    CONSTRAINT unique_app_version
            UNIQUE (app, version)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON "MOLGENIS"."app_version" TO "molgenis";

ALTER TABLE "MOLGENIS"."schema_metadata"
    ADD COLUMN app VARCHAR,
    ADD COLUMN version VARCHAR,
    ADD CONSTRAINT fk_versions
        FOREIGN KEY ("app", "version")
            REFERENCES "MOLGENIS"."app_version" ("app", "version");

