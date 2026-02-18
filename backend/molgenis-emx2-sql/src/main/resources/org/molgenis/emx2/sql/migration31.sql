-- Grant read access to rls_permissions for all roles (needed for RLS policy evaluation)
GRANT SELECT ON "MOLGENIS"."rls_permissions" TO PUBLIC;

-- Create global Admin role if it does not exist
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_ROLE_*/Admin') THEN
    CREATE ROLE "MG_ROLE_*/Admin" NOLOGIN;
  END IF;
  EXECUTE 'GRANT "MG_ROLE_*/Admin" TO ' || quote_ident(current_user) || ' WITH ADMIN OPTION';
END $$;
