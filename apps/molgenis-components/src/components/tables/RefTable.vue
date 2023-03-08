<template>
  <h5 v-if="reference?.name" class="ml-1">{{ reference.name }}</h5>
  <table class="table table-sm">
    <tr v-for="(value, key) in filteredResults(reference)">
      <td class="key border-right">{{ key }}</td>
      <td class="value">
        <div v-if="reference.metaData">
          <DataDisplayCell :data="value" :meta-data="metaDataOfRow(key)" />
        </div>
        <div v-else>{{ value }}</div>
      </td>
    </tr>
  </table>

  <small class="text-black-50" v-if="showDataOwner">
    <div v-if="reference?.mg_insertedBy">
      Inserted by '{{ reference?.mg_insertedBy }}'
      <span v-if="reference?.mg_insertedOn">
        On {{ new Date(reference?.mg_insertedOn).toLocaleString() }}
      </span>
    </div>
    <div v-if="reference?.mg_updatedBy">
      Updated by '{{ reference?.mg_updatedBy }}'
      <span v-if="reference?.mg_updatedOn">
        On {{ new Date(reference?.mg_updatedOn).toLocaleString() }}
      </span>
    </div>
  </small>
  <div v-if="reference?.mg_draft">
    <span class="badge badge-secondary">Draft</span>
  </div>
</template>

<style scoped>
table .key {
  width: 0;
}
</style>

<script lang="ts" setup>
import DataDisplayCell from "./DataDisplayCell.vue";

const props = defineProps({
  reference: {
    type: Object,
    required: true,
  },
  showDataOwner: {
    type: Boolean,
    default: false,
  },
});

function filteredResults(reference: { [key: string]: string }) {
  const filtered = { ...reference };
  delete filtered.name;
  delete filtered.mg_insertedBy;
  delete filtered.mg_insertedOn;
  delete filtered.mg_updatedBy;
  delete filtered.mg_updatedOn;
  delete filtered.mg_draft;
  delete filtered.metaData;
  return filtered;
}

function metaDataOfRow(key: string) {
  return props.reference.metaData.columns.find((row) => row.name === key);
}
</script>
