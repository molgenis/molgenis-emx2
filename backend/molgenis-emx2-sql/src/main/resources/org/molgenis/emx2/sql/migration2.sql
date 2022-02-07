-- will update MG_ROLE_[schema name] from uppercase to case used in the schema name
-- this prevents issue where uppercase(schema name) was the same for multiple schemas, e.g. Test and test.
do
$$
DECLARE
temprow RECORD;
DECLARE
nm VARCHAR;
BEGIN
	FOREACH nm in array array['Viewer','Manager','Owner','Editor'] LOOP
BEGIN
FOR temprow IN
select distinct r.rolname as oldname, 'MG_ROLE_' || s.table_schema || '/' || nm as newname
FROM pg_catalog.pg_roles r
         join "MOLGENIS"."schema_metadata" s ON (r.rolname ILIKE 'MG_ROLE_'||s.table_schema||'/'||nm||'%') LOOP
				IF NOT EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = temprow.newname) THEN
					RAISE NOTICE 'Value: %', temprow.oldname || ' newname ' ||temprow.newname;
EXECUTE 'ALTER ROLE "' || temprow.oldname || '" RENAME TO "' || temprow.newname || '"';
END IF;
END LOOP;
END;
END LOOP;
-- alter RLS policy
ALTER POLICY "schema_metadata_POLICY" ON "MOLGENIS"."schema_metadata" USING (pg_has_role((concat('MG_ROLE_', table_schema, '/Viewer'))::name, 'MEMBER'::text));
END;
$$