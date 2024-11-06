DO
$$
    DECLARE
        column_metadata RECORD;
        columnName TEXT;
    BEGIN
        FOR column_metadata IN SELECT * FROM "MOLGENIS".column_metadata WHERE "columnType" = 'JSONB_ARRAY'
            LOOP
                columnName := '"' || column_metadata.table_schema || '"."' || column_metadata.table_name || '"."' || column_metadata.column_name || '"';
                EXECUTE 'ALTER COLUMN ' || columnName || ' TYPE JSONB USING USING (to_jsonb("' || column_metadata.column_name || '"));';
            END LOOP;
    END;
$$