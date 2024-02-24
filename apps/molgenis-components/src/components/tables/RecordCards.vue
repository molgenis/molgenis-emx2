<template>
  <RowButtonAdd
    v-if="canEdit"
    :tableId="tableId"
    :schemaId="schemaId"
    @close="$emit('reload')"
  />
  <div v-if="!data.length" class="alert alert-warning">No results found</div>
  <div class="card-columns">
    <RowCard
      v-if="data.length"
      v-for="(row, index) in data"
      :key="id + '-' + index"
      :row="row"
      :columns="columns"
      :tableId="tableId"
      :schemaId="schemaId"
      :can-edit="canEdit"
      :template="template"
      @reload="$emit('reload')"
      @click="$emit('click', $event)"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import RowCard from "./RowCard.vue";
import RowButtonAdd from "./RowButtonAdd.vue";

export default defineComponent({
  name: "RecordCards",
  components: { RowCard, RowButtonAdd },
  props: {
    id: { type: String, required: true },
    data: { type: Array, default: () => [] },
    columns: { type: Array, default: () => [] },
    tableId: { type: String, required: true },
    schemaId: { type: String, required: false },
    canEdit: { type: Boolean, default: false },
    template: { type: String, required: false },
  },
});
</script>
