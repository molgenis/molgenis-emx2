DO
$$
    BEGIN
        ALTER TABLE "MOLGENIS"."column_metadata"
            RENAME COLUMN "columnProfiles" to "columnTags";
    EXCEPTION
        WHEN undefined_column THEN
     END;
$$;
