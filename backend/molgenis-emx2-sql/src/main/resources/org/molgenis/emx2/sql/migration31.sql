DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_ROWLEVEL') THEN
        CREATE ROLE "MG_ROWLEVEL" WITH NOLOGIN;
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS "MOLGENIS"."permission_metadata" (
    table_schema VARCHAR NOT NULL,
    role_name VARCHAR NOT NULL,
    table_name VARCHAR NOT NULL,
    editable_columns VARCHAR[],
    readonly_columns VARCHAR[],
    hidden_columns VARCHAR[],
    PRIMARY KEY (table_schema, role_name, table_name)
);

GRANT SELECT ON "MOLGENIS"."permission_metadata" TO PUBLIC;
