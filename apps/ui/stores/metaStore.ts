import metadataGql from "../../nuxt3-ssr/gql/metadata";
import { type ISchemaMetaData } from "../../meta-data-utils/src/types";

const query = moduleToString(metadataGql);

type Resp<T> = {
  data?: Record<string, T> ;
  error?: any;
};

export const useMetaStore = defineStore('meta-data',{

  state: () => {
    return {
      metaData: {
        foo: 'bar'
      }
    }
  },
  
  actions: {
    async fetchSchemaMetaData(schemaId: string) {
      const resp = await $fetch<Resp<ISchemaMetaData>>(`/${schemaId}/graphql`, {
        method: "POST",
        body: {
          query,
        },
      });

      if (resp.error) {
        console.log("handel error ");
        throw createError({
          ...resp.error,
          statusMessage: `Could not fetch metadata for schema ${schemaId}`,
        });
      }

      if(resp.data) {
        return resp.data._schema;
      }
  

      throw createError('Could not fetch metadata for schema, no data found');
        
    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useMetaStore, import.meta.hot))
}