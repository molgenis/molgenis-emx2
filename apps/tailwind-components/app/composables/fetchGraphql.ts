import { createError } from "#imports";

export default async (
  schemaId: string,
  query: string,
  variables: Record<string, unknown>
): Promise<unknown> => {
  const { data } = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
      variables,
    },
  }).catch((error) => {
    console.error(`Could not fetch metadata for schema ${schemaId}, `, error);
    throw createError({
      ...error,
      statusMessage: `Could not fetch graphql for schema ${schemaId}`,
    });
  });

  return data;
};
