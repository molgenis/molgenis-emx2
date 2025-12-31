<template>
  <SideModal :label="column.label" :isVisible="true" @onClose="emit('onClose')">
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
          :tableId="column.refTableId"
          :schemaId="column.refSchemaId || props.schema"
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
import Client, { convertRowToPrimaryKey } from "../../client/client";
import { IRow } from "../../Interfaces/IRow";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import RefTable from "./RefTable.vue";
import SideModal from "./SideModal.vue";
import type { IColumn } from "metadata-utils";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    rows?: IRow[];
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

let loading = ref(true);
let queryResults: Ref<IRow[]> = ref([]);
let errorMessage = ref("");

updateData(rows.value, column.value.refTableId);

watch([column, rows], () => {
  updateData(rows.value, column.value.refTableId);
});

async function updateData(
  rows: IRow[],
  refTableId?: string,
  activeSchemaId?: string
) {
  const activeSchema =
    activeSchemaId || column.value.refSchemaId || props.schema;
  if (activeSchema && refTableId) {
    errorMessage.value = "";
    loading.value = true;
    queryResults.value = await getRowData(activeSchema, rows, refTableId);
    loading.value = false;
  }
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
    const expandLevel = 2;
    const rowKey = await convertRowToPrimaryKey(row, tableId, activeSchema);
    let queryResult = await client
      .fetchRowData(tableId, rowKey, expandLevel)
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
  const refTableId = refColumn.refTableId;
  const activeSchema =
    refColumn.refSchemaId || column.value.refSchemaId || props.schema;
  if (refTableId && activeSchema) {
    const clickedCellPrimaryKeys = [refTableRow[refColumn.id]].flat();
    updateData(clickedCellPrimaryKeys, refTableId, activeSchema);
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
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br />
  <button @click="showModal = !showModal">Toggle modal</button>
</template>
<script setup lang="ts">
import { ref } from "vue";
let showModal = ref(false);
const column = {
  refTableId: "Pet",
  name: "orders",
  refSchemaId: "pet store",
  label: "Orders",
};
</script>
</docs>
