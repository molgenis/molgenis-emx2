<script setup lang="ts">
import { ref, watch } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import InputSearch from "../input/Search.vue";

const props = defineProps<{
  column: IColumn;
  modelValue: IFilterValue | undefined;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

function textValueFromModelValue(mv: IFilterValue | undefined): string {
  if (!mv || mv.operator !== "like") return "";
  return typeof mv.value === "string" ? mv.value : "";
}

const inputText = ref<string>(textValueFromModelValue(props.modelValue));

watch(
  () => props.modelValue,
  (mv) => {
    const next = textValueFromModelValue(mv);
    if (inputText.value !== next) inputText.value = next;
  }
);

const debouncedEmitText = useDebounceFn((val: string) => {
  if (!val) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "like", value: val });
  }
}, 500);

watch(inputText, (val) => debouncedEmitText(val));
</script>

<template>
  <InputSearch
    :id="`filter-text-${column.id}`"
    v-model="inputText"
    :placeholder="`Search ${column.label || column.id}...`"
    :aria-label="column.label || column.id"
    size="tiny"
  />
</template>
