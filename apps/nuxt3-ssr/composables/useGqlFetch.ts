import type { UseFetchOptions } from "nuxt/app";
import { defu } from "defu";
import { type DocumentNode } from "graphql";

export function useGqlFetch<T, E>(
  query: string | Ref<string> | DocumentNode,
  variables?: object,
  schemaId?: string,
  options: UseFetchOptions<T> = {}
) {
  const config = useRuntimeConfig();

  const queryString = isRef(query)
    ? query.value
    : typeof query !== "string"
    ? moduleToString(query)
    : query;

  let body: { query: string; variables?: object } = {
    query: queryString,
  };

  const variablesValue = isRef(variables)
    ? (variables.value as object)
    : variables;

  if (variables) {
    body.variables = variablesValue;
  }
  const schema = schemaId ?? useRoute().params.schema;
  const url = `/${schema}/catalogue/graphql`;
  const defaults: UseFetchOptions<T> = {
    baseURL: config.public.apiBase,
    method: "POST",
    key: `gql-${url}-${queryString}`,
    body,
    onResponseError(_ctx) {
      logError({
        message: "onResponseError fetching data from GraphQL endpoint",
        statusCode: _ctx.response.status,
        data: _ctx.response._data,
      });
    },
  };

  const params = defu(options, defaults);

  // @ts-ignore it cant figure out the combined param types
  return useFetch<T, E>(url, params);
}
