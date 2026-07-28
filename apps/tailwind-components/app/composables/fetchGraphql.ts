import { DATA_NOT_FOUND_ERROR } from "../utils/constants";
import { fetchErrorToNuxtError } from "../utils/fetchErrorToNuxtError";

export default async (
  schemaId: string,
  query: string,
  variables: any,
  options?: { signal?: AbortSignal }
): Promise<any> => {
  const { data } = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
      variables,
    },
    signal: options?.signal,
  }).catch((error) => {
    const message = `Could not fetch graphql for schema ${schemaId}. ${DATA_NOT_FOUND_ERROR}`;
    console.error(message, error);
    throw fetchErrorToNuxtError(error, message);
  });

  return data;
};
