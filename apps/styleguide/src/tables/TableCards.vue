<template>
  <div class="card-columns">
    <RowCard
      v-for="row in data"
      :key="JSON.stringify(row)"
      :row="row"
      :columns="columns"
      :tableName="tableName"
      :can-edit="canEdit"
      @reload="$emit('reload')"
    />
  </div>
</template>

<script>
import RowCard from "./RowCard";

export default {
  components: { RowCard },
  props: {
    data: Array,
    columns: Array,
    tableName: String,
    canEdit: Boolean,
  },
};
</script>
