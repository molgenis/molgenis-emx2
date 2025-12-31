CREATE OR REPLACE FUNCTION "MOLGENIS".interval_to_iso8601(interval_val INTERVAL)
    RETURNS TEXT AS $$
DECLARE
    years TEXT;
    months TEXT;
    days TEXT;
BEGIN
    IF interval_val IS NULL THEN
        RETURN NULL;
    END IF;

    years := CASE WHEN EXTRACT(YEAR FROM interval_val) > 0 THEN EXTRACT(YEAR FROM interval_val)::TEXT || 'Y' ELSE '' END;
    months := CASE WHEN EXTRACT(MONTH FROM interval_val) > 0 THEN EXTRACT(MONTH FROM interval_val)::TEXT || 'M' ELSE '' END;
    days := CASE WHEN EXTRACT(DAY FROM interval_val) > 0 THEN EXTRACT(DAY FROM interval_val)::TEXT || 'D' ELSE '' END;

    RETURN 'P' || years || months || days;
END;
$$ LANGUAGE plpgsql;