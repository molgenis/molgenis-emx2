GRANT SELECT ON "MOLGENIS"."rls_permissions" TO PUBLIC;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'MG_ROLE_*/Admin') THEN
    CREATE ROLE "MG_ROLE_*/Admin" NOLOGIN;
  END IF;
  EXECUTE 'GRANT "MG_ROLE_*/Admin" TO ' || quote_ident(current_user) || ' WITH ADMIN OPTION';
END $$;
