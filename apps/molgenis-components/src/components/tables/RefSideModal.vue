<template>
  <SideModal
    :label="localColumnName"
    :isVisible="true"
    @onClose="emit('onClose')">
    <div v-if="loading">
      <Spinner />
    </div>
    <div v-else>
      <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
      <div v-if="!queryResults.length">
        The selected reference was not found in the database
      </div>
      <div v-for="queryResult in queryResults">
        <RefTable
          :reference="queryResult"
          :showDataOwner="showDataOwner"
          :startsCollapsed="queryResults.length > 1"
          :tableId="column.refTable"
          :schema="column.refSchema || props.schema"
          @ref-cell-clicked="handleRefCellClicked" />
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
import Client from "../../client/client";
import { IColumn } from "../../Interfaces/IColumn";
import { IRow } from "../../Interfaces/IRow";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
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

let localColumnName = ref(column.value.name);
let loading = ref(true);
let queryResults: Ref<IRow[]> = ref([]);
let errorMessage = ref("");

const activeSchema = column.value.refSchema || props.schema;
if (activeSchema && column.value.refTable) {
  updateData(activeSchema, rows.value, column.value.refTable);
}

watch([column, rows], () => {
  localColumnName.value = column.value.name;
  const activeSchema = column.value.refSchema || props.schema;
  if (activeSchema && column.value.refTable) {
    updateData(activeSchema, rows.value, column.value.refTable);
  }
});

async function updateData(activeSchema: string, rows: IRow[], tableId: string) {
  errorMessage.value = "";
  loading.value = true;
  queryResults.value = await getRowData(activeSchema, rows, tableId);
  loading.value = false;
}

async function getRowData(
  activeSchema: string,
  rowKeys: IRow[],
  tableId: string
): Promise<IRow[]> {
  let newQueryResults: IRow[] = [];
  const client = Client.newClient(activeSchema);
  const metadata = await client.fetchTableMetaData(tableId);
  for (const row of rowKeys) {
    const externalSchemaClient = Client.newClient(metadata.externalSchema);
    const expandLevel = 2;
    const queryResult = await externalSchemaClient
      .fetchRowData(tableId, row, expandLevel)
      .catch(errorHandler);
    queryResult.metadata = metadata;
    newQueryResults.push(queryResult);
  }
  return newQueryResults;
}

async function handleRefCellClicked({
  refColumn,
  refTableRow,
}: {
  refColumn: IColumn;
  refTableRow: IRow;
}): Promise<void> {
  const refTableId = refColumn.refTable;
  const activeSchema =
    refColumn.refSchema || column.value.refSchema || props.schema;
  if (refTableId && activeSchema) {
    const clickedCellPrimaryKeys = [refTableRow[refColumn.id]].flat();
    localColumnName.value = refColumn.name;
    updateData(activeSchema, clickedCellPrimaryKeys, refTableId);
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
