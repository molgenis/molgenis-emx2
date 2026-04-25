-- migration 32: fine-grained permissions (populated incrementally)

CREATE OR REPLACE FUNCTION "MOLGENIS".current_user_roles()
RETURNS text[]
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        string_to_array(current_setting('molgenis.current_roles', true), ','),
        ARRAY(
            SELECT regexp_replace(rolname, '^MG_ROLE_', '')
            FROM pg_auth_members
            JOIN pg_roles ON oid = roleid
            WHERE member = (SELECT oid FROM pg_roles WHERE rolname = current_user)
        )
    )
$$;

ALTER ROLE "MG_USER_admin" BYPASSRLS;

