WITH RECURSIVE role_tree AS (
  SELECT r.oid AS role_oid, r.rolname AS role_name, 0 AS depth
  FROM pg_roles r WHERE r.rolname = :role
  UNION ALL
  SELECT parent.oid, parent.rolname, rt.depth + 1
  FROM role_tree rt
  JOIN pg_auth_members m ON m.member = rt.role_oid
  JOIN pg_roles parent ON parent.oid = m.roleid
  WHERE parent.rolname LIKE 'MG_ROLE_%'
)
SELECT
  rt.role_name AS source_role,
  rt.depth,
  t.tablename AS table_name,
  acl.privilege_type AS grant_type,
  rp.select_level,
  rp.insert_rls,
  rp.update_rls,
  rp.delete_rls,
  rp.editable_columns,
  rp.readonly_columns,
  rp.hidden_columns
FROM role_tree rt
CROSS JOIN pg_tables t
LEFT JOIN LATERAL (
  SELECT a.privilege_type
  FROM pg_class c
  JOIN pg_namespace n ON c.relnamespace = n.oid
  CROSS JOIN LATERAL aclexplode(c.relacl) a
  WHERE n.nspname = t.schemaname
    AND c.relname = t.tablename
    AND a.grantee = rt.role_oid
    AND a.privilege_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE')
) acl ON true
LEFT JOIN "MOLGENIS".rls_permissions rp
  ON rp.role_name = rt.role_name
  AND rp.table_schema = t.schemaname
  AND rp.table_name = t.tablename
WHERE t.schemaname = :schema
  AND (acl.privilege_type IS NOT NULL OR rp.role_name IS NOT NULL)
ORDER BY t.tablename, rt.depth, acl.privilege_type
