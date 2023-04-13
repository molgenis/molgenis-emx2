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
import { AxiosError } from "axios";
import { Ref, ref, toRefs, watch } from "vue";
import { IColumn } from "../../Interfaces/IColumn";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { IRow } from "../../Interfaces/IRow";
import { default as Client, default as client } from "../../client/client";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import { deepEqual, getPrimaryKey } from "../utils";
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
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  loading.value = true;
  queryResults.value = await getRowData();
  loading.value = false;
}

async function getRowData(): Promise<IRefModalData[]> {
  console.log("getting row data ", localRows.value);
  let newQueryResults: IRefModalData[] = [];
  const activeSchema = column.value.refSchema || props.schema;
  const externalSchemaClient = Client.newClient(activeSchema);
  if (localTableId.value) {
    const metadata = await externalSchemaClient.fetchTableMetaData(
      localTableId.value
    );
    for (const row of localRows.value) {
      const primaryKey = getPrimaryKey(row, metadata);
      if (primaryKey) {
        const queryResult = await externalSchemaClient
          .fetchRowData(localTableId.value, primaryKey)
          .catch(errorHandler);
        queryResult.metadata = metadata;
        newQueryResults.push(queryResult);
      }
    }
  }
  return newQueryResults;
}

async function handleRefCellClicked(event: {
  refColumn: IColumn;
  rows: IRow[];
}): Promise<void> {
  console.log("Ref cell click: ", event.refColumn, event.rows);
  const refTableId = event.refColumn.refTable;
  if (refTableId) {
    const Client = client.newClient(
      event.refColumn.refSchema || column.value.refSchema || props.schema
    );
    const metadata = await Client.fetchTableMetaData(refTableId);

    Client.fetchTableData(refTableId, {})
      .then((tableData) => {
        localColumnName.value = event.refColumn.name;
        localTableId.value = refTableId;
        const filteredRows = tableData[localTableId.value].filter(
          (row: IRefModalData) => {
            return event.rows.find((eventRow: IRow) => {
              const eventKey = getPrimaryKey(eventRow, metadata);
              const rowKey = getPrimaryKey(row, metadata);
              console.log("event key:", eventRow);
              console.log("row key:", row);
              return eventKey && rowKey && deepEqual(eventKey, rowKey);
            });
          }
        );
        console.log("filtered rows: ", filteredRows);
        localRows.value = filteredRows;
        updateData();
      })
      .catch(errorHandler);
  } else {
    errorMessage.value = "Failed to load reference data";
  }
}

function errorHandler(error: AxiosError) {
  errorMessage.value = `Failed to load reference data, because: ${error.message}`;
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
const column = { refTable: "Pet", name: "orders", refSchema: "pet store" };
</script>
</docs>
