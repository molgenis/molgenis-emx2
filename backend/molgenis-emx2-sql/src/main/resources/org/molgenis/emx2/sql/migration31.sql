CREATE OR REPLACE FUNCTION "MOLGENIS".mg_generate_autoid(
    schema_name TEXT,
    table_name TEXT,
    column_name TEXT,
    charset TEXT,
    id_length INT
) RETURNS TEXT AS $$
DECLARE
    generated_id TEXT;
    charset_len INT;
    i INT;
    max_retries INT := 100;
    attempt INT := 0;
    id_exists BOOLEAN;
    lock_key TEXT;
BEGIN
    charset_len := length(charset);
    IF charset_len = 0 THEN
        RAISE EXCEPTION 'charset must not be empty';
    END IF;
    IF id_length <= 0 THEN
        RAISE EXCEPTION 'id_length must be positive';
    END IF;

    -- Serialize concurrent ID generation for the same table/column
    lock_key := schema_name || '.' || table_name || '.' || column_name;
    PERFORM pg_advisory_xact_lock(hashtext(lock_key));

    LOOP
        -- Generate a random string
        generated_id := '';
        FOR i IN 1..id_length LOOP
            generated_id := generated_id || substr(charset, floor(random() * charset_len + 1)::int, 1);
        END LOOP;

        -- Check uniqueness
        EXECUTE format(
            'SELECT EXISTS(SELECT 1 FROM %I.%I WHERE %I = $1)',
            schema_name, table_name, column_name
        ) INTO id_exists USING generated_id;

        IF NOT id_exists THEN
            RETURN generated_id;
        END IF;

        attempt := attempt + 1;
        IF attempt >= max_retries THEN
            RAISE EXCEPTION 'mg_generate_autoid: failed to generate unique ID after % attempts for %.%.%',
                max_retries, schema_name, table_name, column_name;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
