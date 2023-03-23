<template>
  <h5 class="ml-1">
    <span
      v-for="fragment in getPrimaryKey(reference, reference.metadata)"
      class="mr-1"
    >
      {{ fragment }}
    </span>
  </h5>
  <table class="table table-sm">
    <tr v-for="(value, key) in filteredResults(reference)">
      <td class="key border-right">{{ key }}</td>
      <td class="value">
        <div v-if="reference.metadata">
          <DataDisplayCell :data="value" :meta-data="metadataOfRow(key)" />
        </div>
        <div v-else>{{ value }}</div>
      </td>
    </tr>
  </table>

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
</template>

<style scoped>
table .key {
  width: 0;
}
</style>

<script lang="ts" setup>
import { IRefModalData } from "../../Interfaces/IRefModalData";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import { getPrimaryKey } from "../utils";
import DataDisplayCell from "./DataDisplayCell.vue";

const props = defineProps<{
  reference: IRefModalData;
  showDataOwner?: boolean;
  isCollapsed?: boolean;
}>();

function filteredResults(reference: IRefModalData): Record<string, any> {
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
  const metadata = props.reference.metadata;
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
