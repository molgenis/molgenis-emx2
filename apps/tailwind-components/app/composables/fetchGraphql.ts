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
    const message = `Could not fetch graphql for schema ${schemaId}. Might you need to sign in or ask permission?`;
    console.error(message, error);
    throw createError({
      ...error,
      message,
      status: error.status === 500 ? 404 : error.status,
    });
  });

  return data;
};
