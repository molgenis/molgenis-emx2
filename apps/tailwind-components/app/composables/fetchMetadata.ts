import { StorageSerializers, useSessionStorage } from "@vueuse/core";

import metadataGql from "../../../tailwind-components/app/gql/metadata";
import { type ISchemaMetaData } from "../../../metadata-utils/src/types";
import { createError } from "#app";
import { moduleToString } from "#imports";
import { errorToMessage } from "../utils/errorToMessage";

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
      const fallback = `Could not fetch metadata for schema ${schemaId}`;
      const message = errorToMessage(error, fallback);
      console.error(message, error);
      throw createError({
        ...error,
        statusMessage: message,
      });
    });

    console.log(`Fetching metadata for schema ${schemaId}`);

    // Update the cache
    cached.value = data._schema;
  }

  return cached.value;
};
