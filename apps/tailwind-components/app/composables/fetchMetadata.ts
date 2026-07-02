import { StorageSerializers, useSessionStorage } from "@vueuse/core";

import metadataGql from "../../../tailwind-components/app/gql/metadata";
import { type ISchemaMetaData } from "../../../metadata-utils/src/types";
import { createError } from "nuxt/app";
import { moduleToString } from "#imports";

const query = moduleToString(metadataGql);

export default async (schemaId: string): Promise<ISchemaMetaData> => {
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
        message: `Could not fetch schema: ${schemaId}. Might you need to sign in or ask permission?`,
      });
    });

    cached.value = data._schema;
  }

  return cached.value;
};
