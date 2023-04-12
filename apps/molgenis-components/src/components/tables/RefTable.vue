<template>
  <div class="mb-4">
    <h5 class="ml-1">
      <ObjectDisplay
        v-if="primaryKey"
        :data="primaryKey"
        :meta-data="metadataOfRow(Object.keys(primaryKey)[0])"
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
          <tr v-for="(value, key) in filteredResults">
            <td class="key border-right">{{ key }}</td>
            <td
              @click="
                {
                  onCellClick(value, key);
                }
              "
              class="value"
              :class="{ refType: isRefType(metadataOfRow(key).columnType) }"
            >
              <DataDisplayCell :data="value" :meta-data="metadataOfRow(key)" />
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
          On {{ new Date(reference.mg_insertedOn as string).toLocaleString() }}
        </span>
      </div>
      <div v-if="reference.mg_updatedBy">
        Updated by '{{ reference.mg_updatedBy }}'
        <span v-if="reference.mg_updatedOn">
          On {{ new Date(reference.mg_updatedOn as string).toLocaleString() }}
        </span>
      </div>
    </small>
    <div v-if="reference.mg_draft">
      <span class="badge badge-secondary">Draft</span>
    </div>
  </div>
</template>

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

<script lang="ts" setup>
import { computed, ref, toRefs, defineEmits } from "vue";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import { getPrimaryKey, isRefType } from "../utils";
import ObjectDisplay from "./cellTypes/ObjectDisplay.vue";
import DataDisplayCell from "./DataDisplayCell.vue";
import { IColumn } from "../../Interfaces/IColumn";
import { IRow } from "../../Interfaces/IRow";
import { table } from "console";

interface IFilteredRefModalData {
  [property: string]: string;
}
const props = defineProps<{
  reference: IRefModalData;
  showDataOwner?: boolean;
  startsCollapsed?: boolean;
}>();
const { reference, startsCollapsed } = toRefs(props);

const emit = defineEmits<{
  (
    e: "refCellClicked",
    data: {
      refColumn: IColumn;
      rows: IRow[];
    }
  ): void;
}>();

let filteredResults = computed(() => getFilteredResults(reference.value));
let canCollapse = computed(() => Object.keys(filteredResults.value).length > 5);
let primaryKey = computed(() =>
  getPrimaryKey(reference.value, reference.value.metadata)
);

let collapsed = ref(startsCollapsed.value && canCollapse.value);

function getFilteredResults(
  reference: IRefModalData
): Record<string, IFilteredRefModalData> {
  const filtered: Record<string, any> = { ...reference };
  delete filtered.mg_insertedBy;
  delete filtered.mg_insertedOn;
  delete filtered.mg_updatedBy;
  delete filtered.mg_updatedOn;
  delete filtered.mg_draft;
  delete filtered.metadata;
  return filtered;
}

function metadataOfRow(key: string | number) {
  const metadata = reference.value.metadata;
  if (isMetaData(metadata) && metadata.columns) {
    return (
      metadata.columns.find((column) => column.id === key) || ({} as IColumn)
    );
  } else {
    throw "Error: Metadata for RefTable not found";
  }
}

function isMetaData(
  metadata: ITableMetaData | string
): metadata is ITableMetaData {
  return (<ITableMetaData>metadata).name !== undefined;
}

function onCellClick(
  columnRows: IFilteredRefModalData,
  referenceTable: string
): void {
  const rows: IRow[] = [columnRows].flat(); //[{name:spike}]
  const refColumn = reference.value.metadata.columns?.find((column) => {
    return column.name === referenceTable;
  });

  if (refColumn) {
    emit("refCellClicked", {
      refColumn,
      rows,
    });
  }
}
</script>
