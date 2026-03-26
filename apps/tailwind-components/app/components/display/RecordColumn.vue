<script setup lang="ts">
import type { IColumnDisplay } from "../../../types/types";
import ValueEMX2 from "../value/EMX2.vue";

const props = withDefaults(
  defineProps<{
    column: IColumnDisplay;
    value: any;
    showEmpty?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

function isEmpty(val: any): boolean {
  if (val === null || val === undefined || val === "") return true;
  if (Array.isArray(val) && val.length === 0) return true;
  return false;
}
</script>

<template>
  <template v-if="isEmpty(value) && !showEmpty"></template>
  <span v-else-if="isEmpty(value) && showEmpty" class="text-gray-400 italic">
    not provided
  </span>
  <component
    v-else-if="column.displayComponent"
    :is="column.displayComponent"
    :column="column"
    :value="value"
    :show-empty="showEmpty"
  />
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
