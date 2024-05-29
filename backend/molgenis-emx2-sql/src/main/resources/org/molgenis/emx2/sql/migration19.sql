DO $$
    DECLARE
        schema_name text;
    BEGIN
        -- Loop through each schema in the current database
        FOR schema_name IN
            SELECT s.table_schema
            FROM "MOLGENIS".schema_metadata s
            LOOP
                -- Execute the CREATE COLLATION statement for each schema
                EXECUTE format(
                        'CREATE COLLATION IF NOT EXISTS %I.numeric (provider = icu, locale = ''en-u-kn-true'');',
                        schema_name
                        );
            END LOOP;
    END $$;