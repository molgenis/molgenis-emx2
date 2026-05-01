CREATE TABLE IF NOT EXISTS "MOLGENIS".groups_metadata (
    schema TEXT NOT NULL,
    name TEXT NOT NULL,
    users TEXT[],
    PRIMARY KEY (schema, name),
    FOREIGN KEY (schema) REFERENCES "MOLGENIS".schema_metadata(table_schema)
        ON UPDATE CASCADE ON DELETE CASCADE
);
