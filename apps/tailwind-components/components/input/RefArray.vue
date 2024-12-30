<script setup lang="ts">
import type { ITableDataResponse } from "~/composables/fetchTableData";

const props = withDefaults(
  defineProps<{
    id: string;
    schemaId: string;
    tableId: string;
    labelTemplate: string;
    modelValue?: any[];
    required?: boolean;
    hasError?: boolean;
    placeholder?: string;
    debug?: boolean;
  }>(),
  {
    modelValue: [],
    required: false,
    hasError: false,
    placeholder: "Select an item",
    debug: false,
  }
);
const emit = defineEmits(["update:modelValue"]);
const labelValueMap = ref({} as Record<string, object>[]);
const data: ITableDataResponse = await fetchTableData(
  props.schemaId,
  props.tableId
);
data.rows.forEach(
  (row) => (labelValueMap.value[applyTemplate(props.labelTemplate, row)] = row)
);

function applyTemplate(template: string, row: Record<string, any>[]): string {
  const ids = Object.keys(row);
  const vals = Object.values(row);
  const label = new Function(...ids, "return `" + template + "`;")(...vals);
  return label;
}

const listOptions = computed(() => {
  console.log("listOptions");
  return Object.keys(labelValueMap.value).map((key) => {
    return { name: key };
  });
});

const selectedValues = computed(() => {
  return props.modelValue.map((value) =>
    applyTemplate(props.labelTemplate, value)
  );
});

function handleInput(input) {
  emit(
    "update:modelValue",
    input.map((label) => labelValueMap.value[label])
  );
}
</script>

<template>
  <InputList
    :id="id"
    :nodes="listOptions"
    :modelValue="selectedValues"
    @update:modelValue="handleInput"
  />
  <div v-if="debug">
    <div class="mt-2">
      data:
      <pre>{{ data }}</pre>
    </div>
  </div>
</template>
