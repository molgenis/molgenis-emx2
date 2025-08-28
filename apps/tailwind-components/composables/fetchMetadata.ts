import { StorageSerializers, useSessionStorage } from "@vueuse/core";

import metadataGql from "../../catalogue/gql/metadata";
import { type ISchemaMetaData } from "../../metadata-utils/src/types";
import { createError } from "#app";
import { moduleToString } from "#imports";

const query = moduleToString(metadataGql);

export default async (
  schemaId: string,
  enhance = true
): Promise<ISchemaMetaData> => {
  // Use sessionStorage to cache data
  const cached = useSessionStorage<ISchemaMetaData>(schemaId, null, {
    serializer: StorageSerializers.object,
  });

  if (!cached.value) {
    const { data } = await $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: {
        query,
        variables: {
          enhance: enhance,
        },
      },
    }).catch((error) => {
      console.error(`Could not fetch metadata for schema ${schemaId}, `, error);
      throw createError({
        ...error,
        statusMessage: `Could not fetch metadata for schema ${schemaId}`,
      });
    });

    console.log(`Fetching metadata for schema ${schemaId}`);

    // Update the cache
    cached.value = data._schema;
  }

  return cached.value;
};
