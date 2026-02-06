SELECT json_build_object(
  '@context', json_build_object(
    'dcat', 'http://www.w3.org/ns/dcat#',
    'dct', 'http://purl.org/dc/terms/',
    'foaf', 'http://xmlns.com/foaf/0.1/',
    'fdp-o', 'https://w3id.org/fdp/fdp-o#',
    'datacite', 'http://purl.org/spar/datacite/',
    'rdfs', 'http://www.w3.org/2000/01/rdf-schema#',
    'ldp', 'http://www.w3.org/ns/ldp#'
  ),
  '@id', ${base_url} || '/' || ${schema} || '/api/fdp',
  '@type', 'fdp-o:FAIRDataPoint',
  'dct:title', ${schema},
  'rdfs:label', ${schema},
  'dct:description', 'FAIR Data Point for ' || ${schema},
  'dct:publisher', json_build_object(
    '@id', ${base_url} || '/publisher',
    '@type', 'foaf:Agent',
    'foaf:name', 'MOLGENIS'
  ),
  'dct:license', json_build_object('@id', 'https://creativecommons.org/licenses/by/4.0/'),
  'dct:conformsTo', json_build_object('@id', 'https://www.purl.org/fairtools/fdp/schema/0.1/fdpMetadata'),
  'dcat:endpointURL', json_build_object('@id', ${base_url} || '/' || ${schema} || '/api/fdp'),
  'fdp-o:metadataIdentifier', json_build_object(
    '@id', ${base_url} || '/' || ${schema} || '/api/fdp#identifier',
    '@type', 'datacite:Identifier',
    'datacite:usesIdentifierScheme', json_build_object('@id', 'datacite:url'),
    'rdfs:label', ${base_url} || '/' || ${schema} || '/api/fdp'
  ),
  'fdp-o:metadataIssued', json_build_object(
    '@type', 'http://www.w3.org/2001/XMLSchema#dateTime',
    '@value', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
  ),
  'fdp-o:metadataModified', json_build_object(
    '@type', 'http://www.w3.org/2001/XMLSchema#dateTime',
    '@value', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
  ),
  'fdp-o:conformsToFdpSpec', json_build_object('@id', 'https://specs.fairdatapoint.org/fdp-specs-v1.2.html'),
  'fdp-o:metadataCatalog', COALESCE(
    (
      SELECT json_agg(json_build_object(
        '@id', ${base_url} || '/' || ${schema} || '/api/fdp/catalog/' || r.id
      ))
      FROM "Resources" r
      WHERE r.type && ARRAY['Catalogue', 'Network']::varchar[]
    ),
    '[]'::json
  )
) AS result
