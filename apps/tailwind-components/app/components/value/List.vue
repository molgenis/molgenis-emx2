<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, ColumnType } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "./EMX2.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data: string[] | number[] | Record<string, any>[];
    hideListSeparator?: boolean;
  }>(),
  {
    hideListSeparator: false,
  }
);

// Strip _ARRAY suffix to get element type
const elementType = computed(
  () => props.metadata.columnType.replace("_ARRAY", "") as ColumnType
);

// Create element metadata with single-value type
const elementMetadata = computed<IColumn>(() => ({
  ...props.metadata,
  columnType: elementType.value,
}));

const dataArray = computed(() => props.data as unknown[]);
</script>

<template>
  <template v-for="(element, index) in dataArray" :key="index">
    <ValueEMX2 :metadata="elementMetadata" :data="element" />
    <span v-if="index < dataArray.length - 1 && !hideListSeparator"
      >,&nbsp;</span
    >
  </template>
</template>
