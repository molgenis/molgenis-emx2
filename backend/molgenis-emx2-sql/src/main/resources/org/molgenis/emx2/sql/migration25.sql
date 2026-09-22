UPDATE "MOLGENIS"."schema_metadata"
SET settings = (
    SELECT to_jsonb(
                   jsonb_object_agg(
                           key,
                           CASE
                               WHEN key LIKE 'page.%' THEN jsonb_build_object(
                                       'html', value,  -- Ensures proper JSON string formatting
                                       'css', '',
                                       'javascript', '',
                                       'dependencies', jsonb_build_object(
                                          'css', jsonb_build_array(),
                                          'javascript', jsonb_build_array()
                                        )
                                      )::text
                               ELSE value
                               END
                   )
           )
    FROM jsonb_each_text(settings::jsonb)
)
WHERE settings IS NOT NULL;