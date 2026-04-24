CREATE OR REPLACE FUNCTION "MOLGENIS".mg_enforce_row_authorisation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    caller_roles text[];
    role_name_stripped text;
    allowed_change_owner boolean := false;
    allowed_share boolean := false;
BEGIN
    IF OLD.mg_owner IS NOT DISTINCT FROM NEW.mg_owner
       AND OLD.mg_roles IS NOT DISTINCT FROM NEW.mg_roles THEN
        RETURN NEW;
    END IF;

    caller_roles := "MOLGENIS".current_user_roles();

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_user AND rolbypassrls) THEN
        RETURN NEW;
    END IF;

    IF OLD.mg_owner IS DISTINCT FROM NEW.mg_owner THEN
        FOR role_name_stripped IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM "MOLGENIS"."permission_attributes"
                WHERE role_name = regexp_replace(role_name_stripped, '^MG_ROLE_', '')
                  AND schema_name = TG_TABLE_SCHEMA
                  AND table_name  = TG_TABLE_NAME
                  AND change_owner = true
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
        FOR role_name_stripped IN SELECT unnest(caller_roles) LOOP
            IF EXISTS (
                SELECT 1 FROM "MOLGENIS"."permission_attributes"
                WHERE role_name = regexp_replace(role_name_stripped, '^MG_ROLE_', '')
                  AND schema_name = TG_TABLE_SCHEMA
                  AND table_name  = TG_TABLE_NAME
                  AND share = true
            ) THEN
                allowed_share := true;
                EXIT;
            END IF;
        END LOOP;
        IF NOT allowed_share THEN
            RAISE EXCEPTION 'share not permitted on %.% for current_user %', TG_TABLE_SCHEMA, TG_TABLE_NAME, current_user;
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
