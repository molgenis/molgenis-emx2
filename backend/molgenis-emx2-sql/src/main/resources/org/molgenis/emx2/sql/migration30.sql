CREATE OR REPLACE FUNCTION "MOLGENIS".interval_array_to_iso8601(interval_vals INTERVAL[])
    RETURNS TEXT[] AS $$
DECLARE
    result TEXT[] := '{}';
    i INTERVAL;
BEGIN
    IF interval_vals IS NULL THEN
        RETURN NULL;
    END IF;

    FOREACH i IN ARRAY interval_vals LOOP
            result := array_append(result, "MOLGENIS".interval_to_iso8601(i));
        END LOOP;

    RETURN result;
END;
$$ LANGUAGE plpgsql;