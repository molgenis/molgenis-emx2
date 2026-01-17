<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import ValueRef from "../value/Ref.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    value: any;
    showEmpty?: boolean;
    getRefClickAction?: (col: IColumn, row: any) => () => void;
  }>(),
  {
    showEmpty: false,
  }
);

function isEmpty(val: any): boolean {
  return val === null || val === undefined || val === "";
}

function handleRefClick() {
  if (props.getRefClickAction) {
    props.getRefClickAction(props.column, props.value)();
  }
}
</script>

<template>
  <template v-if="isEmpty(value) && !showEmpty"></template>
  <span v-else-if="isEmpty(value) && showEmpty" class="text-gray-400 italic">
    not provided
  </span>
  <ValueRef
    v-else-if="column.columnType === 'REF'"
    :metadata="column as IRefColumn"
    :data="value"
    @refCellClicked="handleRefClick"
  />
  <span
    v-else-if="
      column.columnType === 'REF_ARRAY' || column.columnType === 'REFBACK'
    "
    class="text-gray-500 italic"
  >
    (list view coming)
  </span>
  <span
    v-else-if="
      column.columnType === 'ONTOLOGY' || column.columnType === 'ONTOLOGY_ARRAY'
    "
    class="text-gray-500 italic"
  >
    (ontology coming)
  </span>
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
