import metadataGql from "../../nuxt3-ssr/gql/metadata";
import { type ISchemaMetaData } from "../../meta-data-utils/src/types";

const query = moduleToString(metadataGql);

export const useMetaStore = defineStore({
  id: 'store',
  state: () => {
    return {
      metaData: {
        foo: 'bar'
      }
    }
  },
  actions: {
    async fetchSchemaMetaData(schemaId: string) {
      const resp = await $fetch(`/${schemaId}/graphql`, {
        method: "POST",
        body: {
          query,
        },
      });


    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useMetaStore, import.meta.hot))
}