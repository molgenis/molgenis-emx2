<template>
  <div class="card-columns">
    <RowCard
      v-for="row in data"
      :key="JSON.stringify(row)"
      :can-edit="canEdit"
      :columns="columns"
      :row="row"
      :table-name="tableName"
      @click="$emit('click', $event)"
      @reload="$emit('reload')"
    />
  </div>
</template>

<script>
import RowCard from './RowCard.vue'

export default {
  components: {RowCard},
  props: {
    data: Array,
    columns: Array,
    tableName: String,
    canEdit: Boolean,
  },
}
</script>
