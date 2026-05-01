CREATE OR REPLACE FUNCTION "MOLGENIS".current_user_groups(schema_name TEXT) RETURNS TEXT[]
    LANGUAGE SQL STABLE AS $$
    SELECT COALESCE(array_agg(name), ARRAY[]::TEXT[])
    FROM "MOLGENIS".groups_metadata gm
    WHERE gm.schema = schema_name
      AND current_user = ANY (gm.users);
$$;
