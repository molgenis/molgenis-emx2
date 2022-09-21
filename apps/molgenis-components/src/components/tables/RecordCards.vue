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
      @click="$emit('click', $event)"
    />
  </div>
</template>

<script>
import RowCard from "./RowCard.vue";

export default {
  name: "RecordCards",
  components: { RowCard },
  props: {
    id: String,
    data: { type: Array, default: () => [] },
    columns: Array,
    tableName: String,
    canEdit: Boolean,
    template: String,
  },
};
</script>
