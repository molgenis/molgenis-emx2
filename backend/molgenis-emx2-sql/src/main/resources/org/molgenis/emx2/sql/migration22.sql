DO
$$
    DECLARE
        column_metadata RECORD;
        tableName TEXT;
        alterStatement TEXT;
        triggerFunctionNamePrefix  TEXT;
    BEGIN
        FOR column_metadata IN SELECT * FROM "MOLGENIS".column_metadata WHERE "columnType" = 'REFBACK'
            LOOP
                tableName := '"' || column_metadata.table_schema || '"."' || column_metadata.table_name || '"';
                triggerFunctionNamePrefix =  column_metadata.table_schema || '"."1' || column_metadata.table_name || '-' || column_metadata.column_name;
                EXECUTE 'DROP FUNCTION IF EXISTS "' || triggerFunctionNamePrefix || '_UPSERT" CASCADE';
                EXECUTE 'DROP FUNCTION IF EXISTS "' || triggerFunctionNamePrefix || '_UPDATE" CASCADE';
                EXECUTE 'DROP FUNCTION IF EXISTS "' || triggerFunctionNamePrefix || '_INSERT" CASCADE';
                EXECUTE 'DROP FUNCTION IF EXISTS "' || triggerFunctionNamePrefix || '_DELETE" CASCADE';
                EXECUTE 'ALTER TABLE ' || tableName || ' DROP COLUMN IF  EXISTS "' || column_metadata.column_name || '"';
            END LOOP;
    END;
$$