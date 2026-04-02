import { StorageSerializers, useSessionStorage } from "@vueuse/core";

import metadataGql from "../../../tailwind-components/app/gql/metadata";
import { type ISchemaMetaData } from "../../../metadata-utils/src/types";
import { createError } from "#app";
import { moduleToString } from "#imports";

const query = moduleToString(metadataGql);

const inflight = new Map<string, Promise<ISchemaMetaData>>();
const resolved = new Map<string, ISchemaMetaData>();

export default async (schemaId: string): Promise<ISchemaMetaData> => {
  if (resolved.has(schemaId)) return resolved.get(schemaId)!;

  const cached = useSessionStorage<ISchemaMetaData>(schemaId, null, {
    serializer: StorageSerializers.object,
  });

  if (cached.value) return cached.value;

  if (!inflight.has(schemaId)) {
    const promise = $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: { query },
    })
      .then(({ data }) => {
        resolved.set(schemaId, data._schema);
        cached.value = data._schema;
        return data._schema as ISchemaMetaData;
      })
      .catch((error) => {
        console.error(
          `Could not fetch metadata for schema ${schemaId}, `,
          error
        );
        throw createError({
          ...error,
          statusMessage: `Could not fetch metadata for schema ${schemaId}`,
        });
      })
      .finally(() => {
        inflight.delete(schemaId);
      });

    inflight.set(schemaId, promise);
  }

  return inflight.get(schemaId)!;
};
