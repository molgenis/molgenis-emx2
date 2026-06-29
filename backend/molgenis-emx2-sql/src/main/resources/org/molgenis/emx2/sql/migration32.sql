DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = '_SYSTEM_'
              AND table_name = 'JobStatus'
        ) THEN
            INSERT INTO
                "_SYSTEM_"."JobStatus" ("name", "JobStatus_TEXT_SEARCH_COLUMN")
            VALUES ('CANCELLED', 'The job has been cancelled .')
            ON CONFLICT ("name") DO NOTHING;
        END IF;
    END $$;