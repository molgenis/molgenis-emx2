<template>
  <div class="mb-4">
    <h5 class="ml-1">
      <span
        v-for="keyFragment in getPrimaryKey(reference, reference.metadata)"
        class="mr-1"
      >
        {{ keyFragment }}
      </span>
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
            <td class="value">
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
</style>

<script lang="ts" setup>
import { computed, ref } from "vue";
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import { getPrimaryKey } from "../utils";
import DataDisplayCell from "./DataDisplayCell.vue";

const { reference, startsCollapsed } = defineProps<{
  reference: IRefModalData;
  showDataOwner?: boolean;
  startsCollapsed?: boolean;
}>();

let filteredResults = computed(() => getFilteredResults(reference));
let canCollapse = computed(() => Object.keys(filteredResults).length > 3);

let collapsed = ref(startsCollapsed && canCollapse);

function getFilteredResults(reference: IRefModalData): Record<string, any> {
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
  const metadata = reference.metadata;
  if (isMetaData(metadata) && metadata.columns) {
    return metadata.columns.find((column) => column.name === key) || {};
  } else {
    throw "Error: Metadata for RefTable not found";
  }
}

function isMetaData(
  metadata: ITableMetaData | string
): metadata is ITableMetaData {
  return (<ITableMetaData>metadata).name !== undefined;
}
</script>
