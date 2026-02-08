import { createError } from "#imports";
import { errorToMessage } from "../utils/errorToMessage";

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
    const fallback = `Could not fetch graphql for schema ${schemaId}`;
    const message = errorToMessage(error, fallback);
    console.error(message, error);
    throw createError({
      ...error,
      statusMessage: message,
    });
  });

  return data;
};
