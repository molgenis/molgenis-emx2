DO $$
BEGIN
  UPDATE "MOLGENIS"."column_metadata"
  SET "columnType" = 'EXTENSION'
  WHERE "columnType" = 'PROFILE';

  UPDATE "MOLGENIS"."column_metadata"
  SET "columnType" = 'EXTENSION_ARRAY'
  WHERE "columnType" = 'PROFILES';
END $$;
