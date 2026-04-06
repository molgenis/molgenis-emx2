DO $$
BEGIN
  UPDATE "MOLGENIS"."column_metadata" SET "columnType" = 'VARIANT' WHERE "columnType" = 'EXTENSION';
  UPDATE "MOLGENIS"."column_metadata" SET "columnType" = 'VARIANT_ARRAY' WHERE "columnType" = 'EXTENSION_ARRAY';
END $$;
