import metadataGql from "../../../tailwind-components/app/gql/metadata";
import { type ISchemaMetaData } from "../../../metadata-utils/src/types";
import { createError } from "#app";
import { moduleToString } from "#imports";

const query = moduleToString(metadataGql);
const cache = new Map<string, ISchemaMetaData>();

export default async (schemaId: string): Promise<ISchemaMetaData> => {
  const cached = cache.get(schemaId);
  if (cached) return cached;

  const { data } = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
    },
  }).catch((error) => {
    console.error(`Could not fetch metadata for schema ${schemaId}, `, error);
    throw createError({
      ...error,
      statusMessage: `Could not fetch metadata for schema ${schemaId}`,
    });
  });

  cache.set(schemaId, data._schema);
  return data._schema;
};
