<script setup lang="ts">
import { ref, watch } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import InputSearch from "../input/Search.vue";

const props = defineProps<{
  column: IColumn;
  modelValue: IFilterValue | undefined;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

function textValueFromModelValue(modelValue: IFilterValue | undefined): string {
  if (!modelValue || modelValue.operator !== "like") return "";
  return typeof modelValue.value === "string" ? modelValue.value : "";
}

const inputText = ref<string>(textValueFromModelValue(props.modelValue));

watch(
  () => props.modelValue,
  (modelValue) => {
    const next = textValueFromModelValue(modelValue);
    if (inputText.value !== next) inputText.value = next;
  }
);

const debouncedEmitText = useDebounceFn((val: string) => {
  if (!val) {
    emit("update:modelValue", undefined);
  } else {
    emit("update:modelValue", { operator: "like", value: val });
  }
}, FILTER_DEBOUNCE);

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
