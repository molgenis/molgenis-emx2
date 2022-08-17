<template>
  <LayoutModal @close="$emit('close')">
    <template v-slot:body>
      {{ subclasses }}
      <span v-for="column in subclassColumns" :key="column.name">
        {{ column.table }} {{ column.name }}
        <RowFormInput v-bind="column" :label="column.name" />
      </span>
    </template>
  </LayoutModal>
</template>

<script>
import { LayoutModal, RowFormInput } from "molgenis-components";

export default {
  components: {
    LayoutModal,
    RowFormInput,
  },
  props: {
    tableName: String,
    subclasses: Array,
    table: Object,
  },
  computed: {
    subclassColumns() {
      return this.table.columns.filter((column) =>
        this.subclasses.includes(column.table)
      );
    },
  },
};
</script>
