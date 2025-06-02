import { acceptHMRUpdate, defineStore } from "pinia";
import metadataGql from "../../catalogue/gql/metadata";
import type {
  ISchemaMetaData,
  ITableMetaData,
} from "../../metadata-utils/src/types";
import { moduleToString } from "../../tailwind-components/utils/moduleToString";
import { createError } from "#app/composables/error";

const query = moduleToString(metadataGql);

type Resp<T> = {
  data?: Record<string, T>;
  error?: any;
};

export const useMetaStore = defineStore("metadata", {
  state: () => {
    return {
      metadata: {} as Record<string, ISchemaMetaData>,
    };
  },

  getters: {
    getTableMeta: (state) => {
      return (schemaId: string, tableId: string) => {
        if (!state.metadata) {
          throw new Error(`The store is not initialized`);
        }
        if (!state.metadata[schemaId]) {
          throw new Error(`Schema with id ${schemaId} not found in store`);
        }
        const tableMeta = state.metadata[schemaId].tables.find(
          (t: ITableMetaData) => t.id.toLowerCase() === tableId.toLowerCase()
        );
        if (!tableMeta) {
          throw new Error(
            `Table with id ${tableId} not found in schema ${schemaId}`
          );
        }
        return tableMeta;
      };
    },
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
        throw createError({
          ...resp.error,
          statusMessage: `Could not fetch metadata for schema ${schemaId}`,
        });
      }

      if (resp.data) {
        return resp.data._schema;
      }

      throw createError("Could not fetch metadata for schema, no data found");
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useMetaStore, import.meta.hot));
}
