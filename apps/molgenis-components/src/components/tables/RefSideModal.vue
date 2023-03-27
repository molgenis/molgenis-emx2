<template>
  <SideModal :label="label" :isVisible="true" @onClose="emit('onClose')">
    <div v-if="loading">
      <Spinner />
    </div>
    <div v-else>
      <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
      <div v-for="(queryResult, index) in queryResults">
        <RefTable
          :reference="queryResult"
          :showDataOwner="showDataOwner"
          :startsCollapsed="index > 0"
        />
      </div>
    </div>
    <template v-slot:footer="slot">
      <ButtonAction @click="slot.close()">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, toRefs, watch } from "vue";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { IRow } from "../../Interfaces/IRow";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import { getPrimaryKey } from "../utils";
import SideModal from "./SideModal.vue";
import RefTable from "./RefTable.vue";
import Spinner from "../layout/Spinner.vue";
import Client from "../../client/client";

const props = withDefaults(
  defineProps<{
    tableId: string;
    label: string;
    showDataOwner?: boolean;
    schema: string;
    rows?: IRow[];
  }>(),
  {
    showDataOwner: false,
    rows: () => [] as IRow[],
  }
);
const { label, rows, tableId, schema } = toRefs(props);

const emit = defineEmits(["onClose"]);

let loading = ref(true);
let queryResults = ref([] as IRefModalData[]);
let errorMessage = ref("");
updateData();
watch([tableId, label, rows], () => {
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  loading.value = true;
  queryResults.value = await getRowData();
  loading.value = false;
}

async function getRowData(): Promise<IRefModalData[]> {
  let newQueryResults: IRefModalData[] = [];
  const externalSchemaClient = Client.newClient(schema.value);

  if (tableId.value !== "") {
    for (const row of rows.value) {
      const metadata = await externalSchemaClient
        .fetchTableMetaData(tableId.value)
        .catch((error) => {
          console.log("error", error);
        });
      const primaryKey = getPrimaryKey(row, metadata);

      if (primaryKey) {
        // NOTE TO SELF: schemaName of this may differ here
        const queryResult = await externalSchemaClient
          .fetchRowData(tableId.value, primaryKey)
          .catch(() => {
            errorMessage.value = "Failed to load reference data";
          });
        queryResult.metadata = metadata;
        newQueryResults.push(queryResult);
      }
    }
  }
  return newQueryResults;
}
</script>

<docs>
<template>
  <RefSideModal
    label="Label"
    tableId="Pet"
    :row="{}"
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br /><button @click="showModal = !showModal">Toggle modal</button>
</template>
<script>
export default {
  data: function () {
    return {
      showModal: false,
    };
  },
};
</script>
</docs>
