<template>
  <div>
    <div v-if="!data.length" class="alert alert-warning">No results found</div>
    <RowCard
      v-else
      v-for="(row, index) in data"
      :key="id + '-' + index"
      :row="row"
      :columns="columns"
      :tableName="tableName"
      :can-edit="canEdit"
      :template="template"
      @reload="$emit('reload')"
      @edit="$emit('edit', $event)"
      @delete="$emit('delete', $event)"
      @click="$emit('click', $event)" />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import RowCard from "./RowCard.vue";

export default defineComponent({
  name: "RecordCards",
  components: { RowCard },
  props: {
    id: { type: String, required: true },
    data: { type: Array, default: () => [] },
    columns: { type: Array, default: () => [] },
    tableName: { type: String, required: true },
    canEdit: { type: Boolean, default: false },
    template: { type: String, required: false },
  },
});
</script>
