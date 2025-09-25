import { defu } from "defu";
// @ts-ignore
import type { DocumentNode } from "graphql";
import { moduleToString, logError, useRuntimeConfig, useFetch } from "#imports";
import { type Ref, isRef } from "vue";
import type { UseFetchOptions } from "#app";

interface UseGqlFetchOptions<T> extends UseFetchOptions<T> {
  variables?: object;
  schemaId?: string;
}

export function useGqlFetch<T, E>(
  query: string | Ref<string> | DocumentNode,
  options: UseGqlFetchOptions<T> = {}
) {
  const queryString = isRef(query)
    ? query.value
    : typeof query !== "string"
    ? moduleToString(query)
    : query;

  let body: { query: string; variables?: object } = {
    // @ts-ignore
    query: queryString,
  };

  if (options.variables) {
    const variablesValue = isRef(options.variables)
      ? (options.variables.value as object)
      : options.variables;
    body.variables = variablesValue;
  }
  const config = useRuntimeConfig();
  const schema = options.schemaId ?? (config.public.schema as string);
  const url = `/${schema}/graphql`;
  const defaults: UseFetchOptions<T> = {
    method: "POST",
    key: `gql-${url}-${queryString}`,
    body,
    onResponseError(_ctx) {
      let errorDetail = "";
      try {
        errorDetail = JSON.stringify(_ctx.response._data);
      } catch (e) {
        errorDetail = "Unknown error";
      }
      logError({
        message: "onResponseError fetching data from GraphQL endpoint",
        statusCode: _ctx.response.status,
        data: { errors: [{ message: errorDetail }] },
      });
    },
  };

  const params = defu(options, defaults);

  // @ts-ignore
  return useFetch<T, E>(url, params);
}
