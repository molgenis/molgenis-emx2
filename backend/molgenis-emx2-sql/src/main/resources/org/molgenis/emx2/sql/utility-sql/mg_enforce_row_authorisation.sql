CREATE OR REPLACE FUNCTION "MOLGENIS".mg_enforce_row_authorisation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    caller_roles text[];
    raw_role text;
    allowed_change_owner boolean := false;
    allowed_change_group boolean := false;
BEGIN
    IF OLD.mg_owner IS NOT DISTINCT FROM NEW.mg_owner
       AND OLD.mg_roles IS NOT DISTINCT FROM NEW.mg_roles THEN
        RETURN NEW;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_user AND rolbypassrls) THEN
        RETURN NEW;
    END IF;

    caller_roles := "MOLGENIS".current_user_roles();

    IF OLD.mg_owner IS DISTINCT FROM NEW.mg_owner THEN
        FOR raw_role IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM pg_policies
                WHERE schemaname = TG_TABLE_SCHEMA
                  AND tablename  = TG_TABLE_NAME
                  AND policyname IN (
                      'MG_P_' || raw_role || '_CHANGEOWNER_ALL',
                      'MG_P_' || raw_role || '_CHANGEOWNER_GROUP',
                      'MG_P_' || raw_role || '_CHANGEOWNER_OWN'
                  )
                  AND (
                      policyname = 'MG_P_' || raw_role || '_CHANGEOWNER_ALL'
                      OR (policyname = 'MG_P_' || raw_role || '_CHANGEOWNER_OWN'
                          AND OLD.mg_owner = current_user)
                      OR (policyname = 'MG_P_' || raw_role || '_CHANGEOWNER_GROUP'
                          AND OLD.mg_roles && "MOLGENIS".current_user_roles())
                  )
            ) THEN
                allowed_change_owner := true;
                EXIT;
            END IF;
        END LOOP;
        IF NOT allowed_change_owner THEN
            RAISE EXCEPTION 'change_owner not permitted on %.% for current_user %', TG_TABLE_SCHEMA, TG_TABLE_NAME, current_user;
        END IF;
    END IF;

    IF OLD.mg_roles IS DISTINCT FROM NEW.mg_roles THEN
        FOR raw_role IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM pg_policies
                WHERE schemaname = TG_TABLE_SCHEMA
                  AND tablename  = TG_TABLE_NAME
                  AND policyname IN (
                      'MG_P_' || raw_role || '_CHANGEGROUP_ALL',
                      'MG_P_' || raw_role || '_CHANGEGROUP_GROUP',
                      'MG_P_' || raw_role || '_CHANGEGROUP_OWN'
                  )
                  AND (
                      policyname = 'MG_P_' || raw_role || '_CHANGEGROUP_ALL'
                      OR (policyname = 'MG_P_' || raw_role || '_CHANGEGROUP_OWN'
                          AND OLD.mg_owner = current_user)
                      OR (policyname = 'MG_P_' || raw_role || '_CHANGEGROUP_GROUP'
                          AND OLD.mg_roles && "MOLGENIS".current_user_roles())
                  )
            ) THEN
                allowed_change_group := true;
                EXIT;
            END IF;
        END LOOP;
        IF NOT allowed_change_group THEN
            RAISE EXCEPTION 'changeGroup not permitted on %.% for current_user %', TG_TABLE_SCHEMA, TG_TABLE_NAME, current_user;
        END IF;
        IF NOT (NEW.mg_roles <@ ARRAY(
            SELECT regexp_replace(unnest(caller_roles), '^MG_ROLE_', '')
        )) THEN
            RAISE EXCEPTION 'mg_roles contains role(s) the caller does not hold';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;
