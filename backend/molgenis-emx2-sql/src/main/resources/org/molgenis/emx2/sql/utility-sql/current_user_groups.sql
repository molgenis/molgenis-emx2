CREATE OR REPLACE FUNCTION "MOLGENIS".current_user_groups(p_schema TEXT) RETURNS TEXT[]
    LANGUAGE SQL STABLE AS $$
    SELECT COALESCE(array_agg(DISTINCT group_name), ARRAY[]::TEXT[])
    FROM "MOLGENIS".group_membership_metadata
    WHERE user_name = regexp_replace(current_user, '^MG_USER_', '')
      AND schema_name = p_schema
      AND group_name IS NOT NULL
$$;
