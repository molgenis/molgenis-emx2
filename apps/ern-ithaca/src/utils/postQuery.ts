/**
 * @name postQuery
 * @description A fetch wrapper for posting GraphQL requests
 *
 * @param url string containing the api endpoint
 * @param query a graphql string
 *
 * @returns json
 */

export async function postQuery(
  url: string,
  query: string
): Promise<Record<string, any>> {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ query: query }),
  });
  return response.json();
}
