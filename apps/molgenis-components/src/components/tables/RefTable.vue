<template>
  <div class="mb-4">
    <h5 class="ml-1">
      <ObjectDisplay
        v-if="primaryKey"
        :data="primaryKey"
        :meta-data="reference.metadata"
        class="mr-1"
      />
      <button
        v-if="canCollapse"
        class="btn p-0 m-0 btn-outline-primary border-0 ml-auto float-right"
        @click="collapsed = !collapsed"
      >
        <i :class="`fas fa-fw fa-angle-${collapsed ? 'up' : 'down'}`"></i>
      </button>
    </h5>
    <div>
      <div :class="{ 'collapsed-table': collapsed }">
        <table class="table table-sm mb-2">
          <tr v-for="(cellValue, cellName) in filteredRow">
            <td class="key border-right">{{ cellName }}</td>
            <td
              @click="onCellClick(cellName)"
              :class="{
                refType: isRefType(metadataOfCell(cellName).columnType),
              }"
            >
              <DataDisplayCell
                :data="cellValue"
                :meta-data="metadataOfCell(cellName)"
              />
            </td>
          </tr>
        </table>
      </div>
      <div
        v-if="collapsed"
        class="collapsed-tag border-top rounded-bottom mb-3"
        @click="collapsed = false"
      >
        <small class="px-3 link-color"> Show all records... </small>
      </div>
    </div>

    <small class="text-black-50" v-if="showDataOwner">
      <div v-if="reference.mg_insertedBy">
        Inserted by '{{ reference.mg_insertedBy }}'
        <span v-if="reference.mg_insertedOn">
          On {{ new Date(reference.mg_insertedOn).toLocaleString() }}
        </span>
      </div>
      <div v-if="reference.mg_updatedBy">
        Updated by '{{ reference.mg_updatedBy }}'
        <span v-if="reference.mg_updatedOn">
          On {{ new Date(reference.mg_updatedOn).toLocaleString() }}
        </span>
      </div>
    </small>
    <div v-if="reference.mg_draft">
      <span class="badge badge-secondary">Draft</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, defineEmits, ref, toRefs } from "vue";
import { IColumn } from "../../Interfaces/IColumn";
import { IRow } from "../../Interfaces/IRow";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import { isRefType } from "../utils";
import DataDisplayCell from "./DataDisplayCell.vue";
import ObjectDisplay from "./cellTypes/ObjectDisplay.vue";
import Client from "../../client/client";

const props = defineProps<{
  reference: IRow;
  tableId?: string;
  schema?: string;
  showDataOwner?: boolean;
  startsCollapsed?: boolean;
}>();
const { reference, startsCollapsed } = toRefs(props);

const emit = defineEmits<{
  (
    e: "refCellClicked",
    data: {
      refColumn: IColumn;
      refTableRow: IRow;
    }
  ): void;
}>();

let filteredRow = computed(() => getFilteredRow(reference.value));
let canCollapse = computed(() => Object.keys(filteredRow.value).length > 5);
const primaryKey = ref({});

if (props.tableId && props.schema) {
  const client = Client.newClient(props.schema);
  client.convertRowToPrimaryKey(reference.value, props.tableId).then((res) => {
    primaryKey.value = res;
  });
}

let collapsed = ref(startsCollapsed.value && canCollapse.value);

function getFilteredRow(reference: IRow): IRow {
  const filtered: Record<string, any> = { ...reference };
  delete filtered.mg_insertedBy;
  delete filtered.mg_insertedOn;
  delete filtered.mg_updatedBy;
  delete filtered.mg_updatedOn;
  delete filtered.mg_draft;
  delete filtered.metadata;
  return filtered;
}

function metadataOfCell(key: string | number): IColumn {
  const metadata = reference.value.metadata;
  if (isMetadata(metadata) && metadata.columns) {
    return (
      metadata.columns.find((column) => column.id === key) || ({} as IColumn)
    );
  } else {
    throw "Error: Metadata for RefTable not found";
  }
}

function isMetadata(
  metadata: ITableMetaData | string
): metadata is ITableMetaData {
  return (<ITableMetaData>metadata).name !== undefined;
}

function onCellClick(cellName: string): void {
  const refTableRow: IRow = reference.value;
  const refColumn = refTableRow.metadata.columns?.find((column: IColumn) => {
    return column.id === cellName;
  });

  if (refColumn && isRefType(refColumn.columnType)) {
    emit("refCellClicked", {
      refColumn,
      refTableRow,
    });
  }
}
</script>

<style scoped>
table .key {
  width: 0;
}
.collapsed-table {
  max-height: 6.5rem;
  overflow: hidden;
}
.collapsed-tag {
  text-align: center;
  color: var(--primary);
  cursor: pointer;
}
.collapsed-tag:hover {
  text-decoration: underline;
}
.collapsed-table .table {
  margin-bottom: 0 !important;
}
.table .refType {
  color: var(--primary);
}
.table .refType:hover {
  background-color: rgba(0, 0, 0, 0.05);
  text-decoration: underline;
}
</style>
