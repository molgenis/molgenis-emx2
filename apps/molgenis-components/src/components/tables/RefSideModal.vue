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
      <div v-if="!queryResults.length">
        The selected reference was not found in the database
      </div>
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
import { IRow } from "../../Interfaces/IRow";
import Client from "../../client/client";
import ButtonAction from "../forms/ButtonAction.vue";
import MessageError from "../forms/MessageError.vue";
import Spinner from "../layout/Spinner.vue";
import { convertToPascalCase, getPrimaryKey } from "../utils";
import RefTable from "./RefTable.vue";
import SideModal from "./SideModal.vue";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";

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
let queryResults: Ref<IRow[]> = ref([]);
let errorMessage = ref("");

const activeSchema = column.value.refSchema || props.schema;
if (activeSchema && localTableId.value) {
  updateData(activeSchema, localRows.value, localTableId.value);
}

watch([column, rows], () => {
  localColumnName.value = column.value.name;
  localTableId.value = column.value.refTable;
  localRows.value = rows.value;
  const activeSchema = column.value.refSchema || props.schema;
  if (activeSchema && localTableId.value) {
    updateData(activeSchema, localRows.value, localTableId.value);
  }
});

async function updateData(activeSchema: string, rows: IRow[], tableId: string) {
  errorMessage.value = "";
  loading.value = true;
  if (activeSchema && localTableId.value) {
    queryResults.value = await getRowData(activeSchema, rows, tableId);
  }
  loading.value = false;
}

async function getRowData(
  activeSchema: string,
  rows: IRow[],
  tableId: string
): Promise<IRow[]> {
  let newQueryResults: IRow[] = [];
  const client = Client.newClient(activeSchema);
  const schemaMetadata = await client.fetchSchemaMetaData();
  const metadata = schemaMetadata.tables.find((metadata: ITableMetaData) => {
    return metadata.name === tableId;
  });
  for (const row of rows) {
    const externalSchemaClient = Client.newClient(metadata.externalSchema);
    const primaryKey = getPrimaryKey(row, metadata, schemaMetadata);
    if (primaryKey) {
      const queryResult = await externalSchemaClient
        .fetchRowData(tableId, primaryKey)
        .catch(errorHandler);
      queryResult.metadata = metadata;
      newQueryResults.push(queryResult);
    }
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
    const client = Client.newClient(activeSchema);
    const clickedCellPrimaryKey = [refTableRow[refColumn.id]].flat();
    let filter: any = {};
    // TODO: fix for clickedCellPrimaryKey always being a [0]
    Object.entries(clickedCellPrimaryKey[0]).forEach(([key, value]) => {
      filter[key] = { equals: value };
    });
    client
      .fetchTableData(refTableId, {
        filter,
      })
      .then((tableData: any) => {
        localColumnName.value = refColumn.name;
        localTableId.value = refTableId;
        const rows: IRow[] = tableData[convertToPascalCase(localTableId.value)];
        localRows.value = rows;
        updateData(activeSchema, rows, refTableId);
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
