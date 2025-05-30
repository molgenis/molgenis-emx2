UPDATE "MOLGENIS"."schema_metadata"
SET settings = updated.new_settings
FROM (
  SELECT
    ctid,
    to_jsonb(jsonb_object_agg(
      key,
      CASE
        WHEN key LIKE 'page.%'
         AND jsonb_typeof(value) = 'object'
         AND jsonb_typeof(value->'dependencies') = 'object'
        THEN jsonb_set(
          value,
          '{dependencies,javascript}',
          (
            SELECT jsonb_agg(jsonb_build_object('url', js_item, 'defer', false))
            FROM jsonb_array_elements_text(value->'dependencies'->'javascript') AS js_item
          ),
          true
        )
        ELSE value
      END
    )) AS new_settings
  FROM "MOLGENIS"."schema_metadata",
       LATERAL jsonb_each(settings::jsonb)
  GROUP BY ctid
) AS updated
WHERE "MOLGENIS"."schema_metadata".ctid = updated.ctid;
