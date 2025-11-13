import { GraphiQL } from "graphiql";
import "graphiql/style.css";

async function fetcher(graphQLParams) {
  const url = "./graphql";
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(graphQLParams),
  });
  return response.json();
}

function App() {
  return <GraphiQL fetcher={fetcher} />;
}

export default App;
