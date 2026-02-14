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
    edit_columns VARCHAR[],
    deny_columns VARCHAR[],
    PRIMARY KEY (table_schema, role_name, table_name)
);

GRANT ALL ON "MOLGENIS"."permission_metadata" TO PUBLIC;
