<template>
  <SideModal :label="localColumn" :isVisible="true" @onClose="emit('onClose')">
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

const properties = withDefaults(
  defineProps<{
    schema?: string;
    tableId: string;
    column: string;
    showDataOwner?: boolean;
    refSchema?: string;
    rows: IRow[];
  }>(),
  {
    showDataOwner: false,
    rows: () => [] as IRow[],
  }
);
const { column, rows, tableId } = toRefs(properties);

const emit = defineEmits(["onClose"]);

let localTableId = ref(tableId.value);
let localColumn = ref(column.value);
let localRows = ref(rows.value);
let loading = ref(true);
let queryResults: Ref<IRefModalData[]> = ref([]);
let errorMessage = ref("");

updateData();

watch([tableId, column, rows], () => {
  localColumn.value = column.value;
  localTableId.value = tableId.value;
  localRows.value = rows.value;
});

watch([localColumn, localTableId, localRows], () => {
  updateData();
});

async function updateData() {
  errorMessage.value = "";
  loading.value = true;
  queryResults.value = await getRowData(localTableId.value);
  loading.value = false;
}

async function getRowData(tableId: string): Promise<IRefModalData[]> {
  let newQueryResults: IRefModalData[] = [];
  const activeSchema = properties.refSchema || properties.schema;
  const externalSchemaClient = Client.newClient(activeSchema);
  if (tableId !== "") {
    for (const row of localRows.value) {
      const metadata = await externalSchemaClient.fetchTableMetaData(tableId);
      const primaryKey = getPrimaryKey(row, metadata);

      if (primaryKey) {
        const queryResult = await externalSchemaClient
          .fetchRowData(tableId, primaryKey)
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
  const Client = client.newClient("pet store");
  Client.fetchTableData(table, {}).then((tableData) => {
    localColumn.value = event.refColumn.name;
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
    column="Label"
    tableId="Pet"
    :row="{}"
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br />
  <button @click="showModal = !showModal">Toggle modal</button>
</template>
<script setup lang="ts">
import { ref } from "vue";
let showModal = ref(false);
</script>
</docs>
