CREATE TABLE IF NOT EXISTS "MOLGENIS"."rls_permissions" (
  role_name VARCHAR NOT NULL,
  table_schema VARCHAR NOT NULL,
  table_name VARCHAR NOT NULL,
  select_level VARCHAR,
  insert_rls BOOLEAN,
  update_rls BOOLEAN,
  delete_rls BOOLEAN,
  grant_permission BOOLEAN,
  editable_columns VARCHAR[],
  readonly_columns VARCHAR[],
  hidden_columns VARCHAR[],
  PRIMARY KEY (table_schema, role_name, table_name)
);
