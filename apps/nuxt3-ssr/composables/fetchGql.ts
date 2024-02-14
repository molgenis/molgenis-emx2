import type { DocumentNode } from "graphql";

export const fetchGql = (
  query: string | DocumentNode,
  variables?: object,
  schemaId?: string
) => {
  const queryValue = typeof query !== "string" ? moduleToString(query) : query;
  let body: { query: string; variables?: object } = {
    query: queryValue,
  };

  if (variables) {
    body.variables = variables;
  }

  const route = useRoute();
  const schema = schemaId ? schemaId : route.params.schema;
  return $fetch(`/${schema}/graphql`, {
    method: "POST",
    body,
  });
};
