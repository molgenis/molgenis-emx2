import { DocumentNode } from "graphql";

export const fetchGql = (
  query: string | DocumentNode,
  variables?: object,
  schemaName?: string
) => {
  const queryValue = typeof query !== "string" ? loadGql(query) : query;
  let body: { query: string; variables?: object } = {
    query: queryValue,
  };
  if (variables) {
    body.variables = variables;
  }

  const route = useRoute();
  const config = useRuntimeConfig();
  const schema = schemaName ? schemaName : route.params.schema;
  return $fetch(`/${schema}/catalogue/graphql`, {
    method: "POST",
    baseURL: config.public.apiBase,
    body,
  }).catch((e) => {
    console.log(e);
    console.log(queryValue);
    throw e;
  });
};
