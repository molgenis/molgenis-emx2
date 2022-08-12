<template>
  <div>
    <RowCard
      v-for="row in data"
      :key="JSON.stringify(row) + template"
      :row="row"
      :columns="columns"
      :tableName="tableName"
      :can-edit="canEdit"
      :template="template"
      @reload="$emit('reload')"
      @click="$emit('click', $event)"
    />
  </div>
</template>

<script>
import RowCard from "./RowCard.vue";

export default {
  components: { RowCard },
  props: {
    data: Array,
    columns: Array,
    tableName: String,
    canEdit: Boolean,
    template: String,
  },
};
</script>
