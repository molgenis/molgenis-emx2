
export async function getSessionInfo () {
  const query = `{
    _session {
      email
      roles
      schemas
      token
    }
  }`
  
  const response = await fetch('/api/graphql', {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ query: query }),
  });
  
  return response.json();
  
}

