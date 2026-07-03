import { createError } from "#imports";

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
    console.error(`Could not fetch metadata for schema ${schemaId}, `, error);
    throw createError({
      ...error,
      statusMessage: `Could not fetch graphql for schema ${schemaId}`,
    });
  });

  return data;
};
