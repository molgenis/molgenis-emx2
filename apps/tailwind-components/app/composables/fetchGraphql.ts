import { createError } from "#imports";

export default async (
  schemaId: string,
  query: string,
  variables: any
): Promise<any> => {
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
