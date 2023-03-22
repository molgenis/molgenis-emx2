<template>
  <SideModal :label="label" :isVisible="true" @onClose="emit('onClose')">
    <div v-if="loading">
      <Spinner />
    </div>
    <div v-else>
      <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>

      <div v-for="queryResult in queryResults">
        <RefTable :reference="queryResult" :showDataOwner="showDataOwner" />
      </div>
    </div>
    <template v-slot:footer="slot">
      <ButtonAction @click="slot.close()">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, toRefs, watch } from "vue";
import { INewClient } from "../../client/IClient";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { IRow } from "../../Interfaces/IRow";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import { getPrimaryKey } from "../utils";
import SideModal from "./SideModal.vue";
import RefTable from "./RefTable.vue";

const props = withDefaults(
  defineProps<{
    tableId: string;
    label: string;
    showDataOwner?: boolean;
    client: INewClient;
    rows?: IRow[];
  }>(),
  {
    showDataOwner: false,
    rows: () => [] as IRow[],
  }
);
const { client, label, rows, tableId } = toRefs(props);

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
  if (tableId.value !== "") {
    for (const row of rows.value) {
      const metaData = await client.value.fetchTableMetaData(tableId.value);
      const primaryKey = getPrimaryKey(row, metaData);
      if (primaryKey) {
        const queryResult = await client.value
          .fetchRowData(tableId.value, primaryKey)
          .catch(() => {
            errorMessage.value = "Failed to load reference data";
          });
        queryResult.metaData = metaData;
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
