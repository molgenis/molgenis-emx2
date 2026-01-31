SELECT json_build_object(
  '@context', json_build_object('my', 'urn:molgenis:', 'dcat', 'http://www.w3.org/ns/dcat#'),
  '@graph', json_agg(json_build_object(
    '@id', 'urn:molgenis:' || id,
    '@type', 'my:Resources',
    'my:id', id,
    'my:name', name,
    'my:description', description
  ))
) AS result
FROM "Resources"
