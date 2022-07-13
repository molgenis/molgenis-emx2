do
$$
BEGIN

IF NOT EXISTS (
    SELECT *
    FROM pg_catalog.pg_tables
    WHERE tablename = 'mg_changelog'
    ) THEN
    CREATE TABLE mg_changelog (
      operation char(1) NOT NULL,
      stamp timestamp NOT NULL,
      userid text NOT NULL,
      tablename text NOT NULL,
      old json,
      new json
    );
END IF;

END;
$$