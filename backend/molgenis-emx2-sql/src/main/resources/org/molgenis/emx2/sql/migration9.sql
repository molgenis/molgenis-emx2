ALTER POLICY "schema_metadata_POLICY" ON "MOLGENIS"."schema_metadata" USING (pg_has_role((concat('MG_ROLE_', table_schema, '/Aggregator'))::name, 'MEMBER'::text));
