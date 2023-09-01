import { StorageSerializers, useSessionStorage } from "@vueuse/core";
import { ISchemaMetaData } from "~/interfaces/types";
import query from "~~/gql/metadata";

if (query.loc?.source.body === undefined) {
  throw "unable to load query: " + query.toString();
}
const queryValue = query.loc?.source.body;

export default async (): Promise<ISchemaMetaData> => {
  const route = useRoute();
  const config = useRuntimeConfig();

  const schema = route.params.schema.toString();

  // Use sessionStorage to cache data
  const cached = useSessionStorage<ISchemaMetaData>(schema, null, {
    serializer: StorageSerializers.object,
  });

  if (!cached.value) {
    const resp = await $fetch(`/${schema}/catalogue/graphql`, {
      method: "POST",
      baseURL: config.public.apiBase,
      body: {
        query: queryValue,
      },
    });

    console.log(resp);
    const { data, error } = resp;

    if (error) {
      console.log("handel error ");
      throw createError({
        ...error,
        statusMessage: `Could not fetch metadata for schema ${schema}`,
      });
    }

    // Update the cache
    cached.value = data._schema;
  } else {
    console.log(`Getting value from cache for schema ${schema}`);
  }

  return cached.value;
};
