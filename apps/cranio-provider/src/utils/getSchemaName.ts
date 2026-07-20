export async function getSchemaName(): Promise<string> {
  const query = `{ _schema { name }}`;
  const response = await fetch("../api/graphql", {
    method: "POST",
    body: JSON.stringify({ query: query }),
  });
  const responseJson = await response.json();
  return responseJson.data._schema.name;
}
