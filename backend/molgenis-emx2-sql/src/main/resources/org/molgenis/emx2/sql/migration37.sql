DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM "MOLGENIS".users_metadata
            GROUP BY lower(trim(username))
            HAVING count(*) > 1
        ) THEN
            RAISE EXCEPTION 'duplicate usernames found (case/whitespace-insensitive)';
        END IF;

        UPDATE "MOLGENIS".users_metadata
        SET username = lower(trim(username))
        WHERE username != lower(trim(username));
    END
$$;
