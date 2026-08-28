<script setup lang="ts">
import { computed } from "vue";
import type {
  columnValue,
  IColumn,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import ValueEMX2 from "../value/EMX2.vue";

const props = defineProps<{
  metadata: IColumn;
  value: columnValue;
}>();

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const isEmpty = computed(() => {
  const value = props.value;
  if (value === null || value === undefined || value === "") {
    return true;
  }
  return Array.isArray(value) && value.length === 0;
});
</script>

<template>
  <span v-if="isEmpty" class="text-disabled italic">not provided</span>
  <ValueEMX2
    v-else
    :metadata="metadata"
    :data="value"
    @valueClick="$emit('valueClick', $event)"
  />
</template>
