<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type {
  IColumn,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import { TEXT_FILTER_TYPES, trimmedTerms } from "../../utils/filterUtils";
import InputArray from "../input/Array.vue";
import InputSearch from "../input/Search.vue";

const props = defineProps<{
  column: IColumn;
  modelValue: IFilterValue | undefined;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: IFilterValue | undefined];
}>();

const label = computed(() => props.column.label || props.column.id);
const isMultiValue = computed(() =>
  TEXT_FILTER_TYPES.has(props.column.columnType)
);

function termsFromModelValue(modelValue: IFilterValue | undefined): string[] {
  return modelValue?.operator === "like" ? modelValue.value : [];
}

function rowsFromTerms(terms: string[]): columnValue[] {
  return terms.length > 0 ? [...terms] : [""];
}

const rows = ref<columnValue[]>(
  rowsFromTerms(termsFromModelValue(props.modelValue))
);
const rowsVersion = ref(0);
const singleText = computed({
  get: () => String(rows.value[0] ?? ""),
  set: (text: string) => {
    rows.value = [text];
  },
});

watch(
  () => props.modelValue,
  (modelValue) => {
    const incoming = termsFromModelValue(modelValue);
    if (termsFromRows(rows.value).join("\n") === incoming.join("\n")) return;
    rows.value = rowsFromTerms(incoming);
    rowsVersion.value++;
  }
);

function termsFromRows(values: columnValue[]): string[] {
  return trimmedTerms(values.map((value) => String(value ?? "")));
}

const debouncedEmit = useDebounceFn((values: columnValue[]) => {
  const terms = termsFromRows(values);
  emit(
    "update:modelValue",
    terms.length > 0 ? { operator: "like", value: terms } : undefined
  );
}, FILTER_DEBOUNCE);

watch(rows, (values) => debouncedEmit(values), { deep: true });
</script>

<template>
  <div role="group" :aria-label="label">
    <InputArray
      v-if="isMultiValue"
      :key="rowsVersion"
      :id="`filter-text-${column.id}`"
      v-model="rows"
      type="STRING_ARRAY"
      :error-message="null"
      :placeholder="`Search ${label}...`"
    />
    <InputSearch
      v-else
      :id="`filter-text-${column.id}`"
      v-model="singleText"
      :placeholder="`Search ${label}...`"
      :aria-label="label"
    />
  </div>
</template>
