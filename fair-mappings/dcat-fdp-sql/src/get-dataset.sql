SELECT json_build_object(
  '@context', json_build_object(
    'dcat', 'http://www.w3.org/ns/dcat#',
    'dct', 'http://purl.org/dc/terms/',
    'foaf', 'http://xmlns.com/foaf/0.1/',
    'fdp-o', 'https://w3id.org/fdp/fdp-o#',
    'datacite', 'http://purl.org/spar/datacite/',
    'rdfs', 'http://www.w3.org/2000/01/rdf-schema#'
  ),
  '@id', ${base_url} || '/' || ${schema} || '/api/fdp/dataset/' || r.id,
  '@type', 'dcat:Dataset',
  'dct:title', r.name,
  'dct:description', r.description,
  'dct:isPartOf', (
    SELECT json_build_object('@id', ${base_url} || '/' || ${schema} || '/api/fdp/catalog/' || replace(cat.id, ' ', '%20'))
    FROM "Resources" cat
    WHERE r.id = ANY(cat."data resources")
      AND cat.type && ARRAY['Catalogue', 'Network']::varchar[]
    LIMIT 1
  ),
  'dct:license', json_build_object('@id', 'https://creativecommons.org/licenses/by/4.0/'),
  'dct:conformsTo', json_build_object('@id', 'https://www.purl.org/fairtools/fdp/schema/0.1/datasetMetadata'),
  'dct:publisher', json_build_object(
    '@id', ${base_url} || '/publisher',
    '@type', 'foaf:Agent',
    'foaf:name', 'MOLGENIS'
  ),
  'fdp-o:metadataIdentifier', json_build_object(
    '@id', ${base_url} || '/' || ${schema} || '/api/fdp/dataset/' || r.id || '#identifier',
    '@type', 'datacite:Identifier',
    'datacite:usesIdentifierScheme', json_build_object('@id', 'datacite:url'),
    'rdfs:label', ${base_url} || '/' || ${schema} || '/api/fdp/dataset/' || r.id
  ),
  'fdp-o:metadataIssued', json_build_object(
    '@type', 'http://www.w3.org/2001/XMLSchema#dateTime',
    '@value', to_char(COALESCE(r."mg_insertedOn", NOW()), 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
  ),
  'fdp-o:metadataModified', json_build_object(
    '@type', 'http://www.w3.org/2001/XMLSchema#dateTime',
    '@value', to_char(COALESCE(r."mg_updatedOn", NOW()), 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
  ),
  'foaf:homepage', CASE WHEN r.website IS NOT NULL
    THEN json_build_object('@id', r.website)
    ELSE NULL
  END
) AS result
FROM "Resources" r
WHERE r.id = ${id}
  AND r.type && ARRAY['Cohort study', 'Biobank', 'Data source', 'Registry']::varchar[]
