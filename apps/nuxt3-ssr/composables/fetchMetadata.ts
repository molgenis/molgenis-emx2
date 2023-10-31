import { StorageSerializers, useSessionStorage } from "@vueuse/core";
import { ISchemaMetaData } from "meta-data-utils";

import metadataGql from "~~/gql/metadata";

const query = moduleToString(metadataGql);

export default async (schemaName: string): Promise<ISchemaMetaData> => {
  const config = useRuntimeConfig();

  // Use sessionStorage to cache data
  const cached = useSessionStorage<ISchemaMetaData>(schemaName, null, {
    serializer: StorageSerializers.object,
  });

  if (!cached.value) {
    const resp = await $fetch(`/${schemaName}/graphql`, {
      method: "POST",
      baseURL: config.public.apiBase,
      body: {
        query,
      },
    });

    const { data, error } = resp;

    if (error) {
      console.log("handel error ");
      throw createError({
        ...error,
        statusMessage: `Could not fetch metadata for schema ${schemaName}`,
      });
    }

    // Update the cache
    cached.value = data._schema;
  } else {
    console.log(`Getting value from cache for schema ${schemaName}`);
  }

  return cached.value;
};
