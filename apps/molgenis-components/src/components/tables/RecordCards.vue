<template>
  <div>
    <RowCard
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
    data: Array,
    columns: Array,
    tableName: String,
    canEdit: Boolean,
    template: String,
  },
};
</script>
