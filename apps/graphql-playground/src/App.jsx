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

const defaultQuery = `# Welcome to GraphiQL@MOLGENIS
#
# GraphiQL is an in-browser tool for writing, validating, and testing
# GraphQL queries.
#
# Type queries into this side of the screen, and you will see intelligent
# typeaheads aware of the current GraphQL type schema and live syntax and
# validation errors highlighted within the text.
#
# GraphQL queries typically start with a "{" character. Lines that start
# with a # are ignored.
#
# An example GraphQL query might look like:
#
# {Pet(filter:{category:  {
#  name:  {
#      equals: ["cat"]
#   }
# }}){name}}
#
# Keyboard shortcuts:
#
#   Prettify query:  Shift-Ctrl-P (or press the prettify button)
#
#  Merge fragments:  Shift-Ctrl-M (or press the merge button)
#
#        Run Query:  Cmd-Enter (or press the play button)
#
#    Auto Complete:  Space (or just start typing)
#
`;

function App() {
  return <GraphiQL fetcher={fetcher} defaultQuery={defaultQuery} />;
}

export default App;
