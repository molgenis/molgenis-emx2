SELECT
  t.tablename AS table_name,
  has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'SELECT') AS can_select,
  has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'INSERT') AS can_insert,
  has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'UPDATE') AS can_update,
  has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'DELETE') AS can_delete,
  rp.select_level,
  COALESCE(rp.insert_rls, false) AS insert_rls,
  COALESCE(rp.update_rls, false) AS update_rls,
  COALESCE(rp.delete_rls, false) AS delete_rls,
  rp.grant_permission,
  rp.editable_columns,
  rp.readonly_columns,
  rp.hidden_columns
FROM pg_tables t
LEFT JOIN "MOLGENIS".rls_permissions rp
  ON rp.role_name = :role
  AND rp.table_schema = t.schemaname
  AND rp.table_name = t.tablename
WHERE t.schemaname = :schema
  AND (has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'SELECT')
    OR has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'INSERT')
    OR has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'UPDATE')
    OR has_table_privilege(:role, format('%I.%I', :schema, t.tablename), 'DELETE')
    OR rp.select_level IS NOT NULL)
