ALTER TABLE "MOLGENIS".table_metadata ALTER COLUMN table_inherits TYPE VARCHAR[] USING (CASE WHEN table_inherits IS NULL THEN NULL ELSE ARRAY[table_inherits] END);
