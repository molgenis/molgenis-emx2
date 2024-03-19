DO
$$
    DECLARE
        column_metadata RECORD;
        alterStatement  TEXT;
    BEGIN
        FOR column_metadata IN SELECT * FROM "MOLGENIS".column_metadata WHERE "columnType" = 'FILE'
            LOOP
                alterStatement := 'ALTER TABLE "' || column_metadata.table_schema || '"."' || column_metadata.table_name
                                      || '" ADD COLUMN IF NOT EXISTS ' || column_metadata.column_name || '_filename varchar;';
                EXECUTE alterStatement;
            END LOOP;
    END;
$$