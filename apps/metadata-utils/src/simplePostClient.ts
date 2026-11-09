/**
 * A simple, synchronous GraphQL client for use inside computed, visible,
 * required and validation expressions.
 * Note: don't overuse this, the API call is blocking.
 */
export function simplePostClient(
  query: string,
  variables: object,
  schemaId?: string
) {
  const xmlHttp = new XMLHttpRequest();
  xmlHttp.open(
    "POST",
    schemaId ? "/" + schemaId + "/graphql" : "graphql",
    false
  );
  xmlHttp.send(JSON.stringify({ query, variables }));
  return JSON.parse(xmlHttp.responseText).data;
}
