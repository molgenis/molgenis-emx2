import type { UseFetchOptions } from "nuxt/app";
import { defu } from "defu";
import { type DocumentNode } from "graphql";

interface UseGqlFetchOptions<T> extends UseFetchOptions<T> {
  variables?: object;
  schemaId?: string;
}

export function useGqlFetch<T, E>(
  query: string | Ref<string> | DocumentNode,
  options: UseGqlFetchOptions<T> = {}
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

  if (options.variables) {
    const variablesValue = isRef(options.variables)
      ? (options.variables.value as object)
      : options.variables;
    body.variables = variablesValue;
  }
  const schema = options.schemaId ?? useRoute().params.schema;
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
  return useFetch<T, E>(url, params) as UseFetchReturn<T, E>;
}
