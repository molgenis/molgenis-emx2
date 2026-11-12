DO $$
    DECLARE u RECORD;

    BEGIN
        IF EXISTS (
            SELECT 1
            FROM "MOLGENIS".users_metadata
            GROUP BY lower(trim(username))
            HAVING count(*) > 1
        ) THEN
            RAISE EXCEPTION 'duplicate usernames found (trimmed/case-insensitive)';
        END IF;

        FOR u IN
            SELECT username
            FROM "MOLGENIS".users_metadata
            WHERE username <> lower(trim(username))
            LOOP
                EXECUTE format(
                        'ALTER ROLE %I RENAME TO %I',
                        'MG_USER_' || u.username,
                        'MG_USER_' || lower(trim(u.username))
                        );
            END LOOP;

        UPDATE "MOLGENIS".users_metadata
        SET username = lower(trim(username))
        WHERE username <> lower(trim(username));
    END
$$;