UPDATE "MOLGENIS"."column_metadata"
SET "columnSemantics" = (
    SELECT array_agg(
                   CASE
                       WHEN semantic LIKE 'tag:%'
                           OR semantic LIKE 'urn:%'
                           THEN '<' || semantic || '>'
                       ELSE semantic
                       END
                       ORDER BY ordinality
           )
    FROM unnest("column_metadata"."columnSemantics") WITH ORDINALITY AS u(semantic, ordinality)
);

UPDATE "MOLGENIS"."table_metadata"
SET "table_semantics" = (
    SELECT array_agg(
                   CASE
                       WHEN semantic LIKE 'tag:%'
                           OR semantic LIKE 'urn:%'
                           THEN '<' || semantic || '>'
                       ELSE semantic
                       END
                       ORDER BY ordinality
           )
    FROM unnest("table_metadata"."table_semantics") WITH ORDINALITY AS u(semantic, ordinality)
);