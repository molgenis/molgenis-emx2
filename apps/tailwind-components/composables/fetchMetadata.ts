import { StorageSerializers, useSessionStorage } from "@vueuse/core";

import metadataGql from "../../catalogue/gql/metadata";
import { type ISchemaMetaData } from "../../metadata-utils/src/types";

const query = moduleToString(metadataGql);

export default async (schemaId: string): Promise<ISchemaMetaData> => {
  // Use sessionStorage to cache data
  const cached = useSessionStorage<ISchemaMetaData>(schemaId, null, {
    serializer: StorageSerializers.object,
  });

  if (!cached.value) {
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

    
    console.log(`Fetching metadata for schema ${schemaId}`);



    // Update the cache
    cached.value = data._schema;
  } else {
    console.log(`Getting value from cache for schema ${schemaId}`);
  }

  return cached.value;
};
