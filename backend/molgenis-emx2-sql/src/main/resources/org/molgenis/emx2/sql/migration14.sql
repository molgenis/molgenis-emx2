UPDATE
    "MOLGENIS"."column_metadata"
SET "ref_table"=NULL
WHERE "columnType" != 'REF'
  AND "columnType" != 'REF_ARRAY'
  AND "columnType" != 'ONTOLOGY'
  AND "columnType" != 'ONTOLOGY_ARRAY'
  AND "columnType" != 'REFBACK'