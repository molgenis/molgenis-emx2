import { type ISchemaMetaData } from "../../../metadata-utils/src/types";
import metadataGql from "../../../tailwind-components/app/gql/metadata";
import { DATA_NOT_FOUND_ERROR } from "../utils/constants";
import { toApiError } from "../utils/apiError";
import { moduleToString } from "../utils/moduleToString";

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
    throw toApiError(
      error,
      `Could not fetch schema: ${schemaId}. ${DATA_NOT_FOUND_ERROR}`
    );
  });

  cache.set(schemaId, data._schema);
  return data._schema;
};
