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
    <template v-slot:footer>
      <ButtonAction @click="emit('onClose')">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, toRefs, watch } from "vue";
import MessageError from "../forms/MessageError.vue";
import ButtonAction from "../forms/ButtonAction.vue";
import SideModal from "./SideModal.vue";
import { getPrimaryKey } from "../utils";
import { IRow } from "../../Interfaces/IRow";
import { INewClient } from "../../client/IClient";

const props = withDefaults(
  defineProps<{
    tableId: string;
    label: string;
    rows: IRow[];
    showDataOwner?: boolean;
    client: INewClient;
  }>(),
  {
    showDataOwner: false,
  }
);
const { client, label, rows, tableId } = toRefs(props);
if (rows && rows.value === undefined) rows.value = []; // FIXME: set as default

const emit = defineEmits(["onClose"]);

let loading = ref(true);
let queryResults = ref([{ name: String }]);
let errorMessage = ref("");

updateData();
watch([tableId, label, rows], () => {
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  queryResults.value = [];
  loading.value = true;
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
        queryResults.value.push(queryResult);
      }
    }
  }
  loading.value = false;
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
