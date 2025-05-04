import { useRoute } from "#app";
import { moduleToString } from "#imports";
import type { DocumentNode } from "graphql";
import type { GqlResp } from "~/interfaces/types";

export function fetchGql<T>(
  query: string | DocumentNode,
  variables?: object,
  schemaId?: string
): Promise<GqlResp<T>> {
  const queryValue = typeof query !== "string" ? moduleToString(query) : query;
  let body: { query: string; variables?: object } = {
    query: queryValue,
  };

  if (variables) {
    body.variables = variables;
  }

  const route = useRoute();
  const schema = schemaId ? schemaId : route.params.schema;
  return $fetch<GqlResp<T>>(`/${schema}/graphql`, {
    method: "POST",
    body,
  });
}
