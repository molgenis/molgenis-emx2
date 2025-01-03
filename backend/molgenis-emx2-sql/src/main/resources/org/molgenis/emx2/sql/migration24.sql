CREATE OR REPLACE FUNCTION "MOLGENIS".get_expanded_ontology_terms(
    schema_name TEXT,
    table_name TEXT,
    input_terms VARCHAR[]
)
    RETURNS VARCHAR[] AS $$
DECLARE
    result_terms VARCHAR[]; -- Array to store the result
BEGIN
    EXECUTE
        'WITH RECURSIVE term_hierarchy AS (
            -- Start with the given terms
            SELECT name
            FROM "' || schema_name || '"."' || table_name || '"
        WHERE name = ANY($1)

        UNION

        -- Recursively find terms linked via ''parent''
        SELECT t.name
        FROM "' || schema_name || '"."' || table_name || '" t
        INNER JOIN term_hierarchy th ON t.parent = th.name
    )
    SELECT array_agg(name)
    FROM term_hierarchy'
        INTO result_terms
        USING input_terms;
    RETURN COALESCE(result_terms,ARRAY[]::varchar[]);
END;
$$ LANGUAGE plpgsql;