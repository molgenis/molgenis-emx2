<template>
  <SideModal
    :label="localColumnName"
    :isVisible="true"
    @onClose="emit('onClose')"
  >
    <div v-if="loading">
      <Spinner />
    </div>
    <div v-else>
      <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
      <div v-for="queryResult in queryResults">
        <RefTable
          :reference="queryResult"
          :showDataOwner="showDataOwner"
          :startsCollapsed="queryResults.length > 1"
          @ref-cell-clicked="handleRefCellClicked"
        />
      </div>
    </div>
    <template v-slot:footer="slot">
      <ButtonAction @click="slot.close()">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { Ref, ref, toRefs, watch } from "vue";
import { IColumn } from "../../Interfaces/IColumn";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { IRow } from "../../Interfaces/IRow";
import { default as Client, default as client } from "../../client/client";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import { getPrimaryKey } from "../utils";
import RefTable from "./RefTable.vue";
import SideModal from "./SideModal.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    rows: IRow[];
    schema?: string;
    showDataOwner?: boolean;
  }>(),
  {
    showDataOwner: false,
    rows: () => [] as IRow[],
  }
);
const { column, rows } = toRefs(props);

const emit = defineEmits(["onClose"]);

let localTableId = ref(column.value.refTable);
let localColumnName = ref(column.value.name);
let localRows = ref(rows.value);
let loading = ref(true);
let queryResults: Ref<IRefModalData[]> = ref([]);
let errorMessage = ref("");

updateData();

watch([column, rows], () => {
  localColumnName.value = column.value.name;
  localTableId.value = column.value.refTable;
  localRows.value = rows.value;
});

watch([localColumnName, localTableId, localRows], () => {
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
  const activeSchema = column.value.refSchema || props.schema;
  const externalSchemaClient = Client.newClient(activeSchema);
  if (localTableId.value) {
    const metadata = await externalSchemaClient.fetchTableMetaData(
      localTableId.value
    );
    for (const row of localRows.value) {
      const primaryKey = getPrimaryKey(row, metadata);
      console.log(primaryKey);
      if (primaryKey) {
        const queryResult = await externalSchemaClient
          .fetchRowData(localTableId.value, primaryKey)
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

function handleRefCellClicked(event: {
  refColumn: IColumn;
  rows: any[];
}): void {
  const table = event.refColumn.refTable || "";
  const Client = client.newClient(
    event.refColumn.refSchema || column.value.refSchema || props.schema
  );
  Client.fetchTableData(table, {}).then((tableData) => {
    localColumnName.value = event.refColumn.name;
    localTableId.value = table;
    const filteredRows = tableData[localTableId.value].filter((row: IRow) => {
      return event.rows.find((eventRow) => eventRow.name === row.name);
    });
    localRows.value = filteredRows;
  });
}
</script>

<docs>
<template>
  <RefSideModal
    :column="column"
    :rows="[]"
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br />
  <button @click="showModal = !showModal">Toggle modal</button>
</template>
<script setup lang="ts">
import { ref } from "vue";
let showModal = ref(false);
const column = {refTable: "Pet", name: "orders", refSchema: 'pet store'}
</script>
</docs>
