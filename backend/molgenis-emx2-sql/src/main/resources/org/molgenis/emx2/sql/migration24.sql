CREATE OR REPLACE FUNCTION "MOLGENIS".get_terms_including_children(
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
    SELECT array_agg(DISTINCT name)
    FROM term_hierarchy'
        INTO result_terms
        USING input_terms;
    RETURN COALESCE(result_terms,ARRAY[]::varchar[]);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION "MOLGENIS".get_terms_including_parents(
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
            SELECT name,parent
            FROM "' || schema_name || '"."' || table_name || '"
            WHERE name = ANY($1)

            UNION ALL

            -- Recursively find terms linked via ''parent''
            SELECT t.name,t.parent
            FROM "' || schema_name || '"."' || table_name || '" t
            INNER JOIN term_hierarchy th ON th.parent = t.name
            WHERE th.parent IS NOT NULL
        )
        SELECT COALESCE(array_agg(DISTINCT name), ''{}''::VARCHAR[])
        FROM term_hierarchy'
        INTO result_terms
        USING input_terms;
    RETURN COALESCE(result_terms, ARRAY[]::varchar[]);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION "MOLGENIS".ilike_terms_including_parents(
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
            -- Start with terms matching the pattern
            SELECT name, parent
            FROM "' || schema_name || '"."' || table_name || '"
            WHERE ' ||
            array_to_string(ARRAY(
                SELECT format('name ILIKE %L', '%' || term || '%')
                FROM unnest(input_terms) AS term
            ), ' OR ')
            || '
            UNION ALL

            -- Recursively find terms linked via ''parent''
            SELECT t.name, t.parent
            FROM "' || schema_name || '"."' || table_name || '" t
            INNER JOIN term_hierarchy th ON th.parent = t.name
            WHERE th.parent IS NOT NULL
        )
        SELECT COALESCE(array_agg(DISTINCT name), ''{}''::VARCHAR[])
        FROM term_hierarchy'
        INTO result_terms
        USING input_terms;
    RETURN COALESCE(result_terms, ARRAY[]::VARCHAR[]);
END;
$$ LANGUAGE plpgsql;
