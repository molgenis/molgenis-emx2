<script setup lang="ts">
import type {
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import Input from "../Input.vue";

const props = withDefaults(
  defineProps<{
    id: string;
    inputType: CellValueType;
    legend?: string;
    minLabel?: string;
    maxLabel?: string;
  }>(),
  {
    minLabel: "Min",
    maxLabel: "Max",
  }
);

const modelValue = defineModel<[columnValue, columnValue]>({
  default: () => [null, null],
});

function updateMin(val: columnValue | columnValue[]) {
  modelValue.value = [
    (Array.isArray(val) ? val[0] : val) ?? null,
    modelValue.value[1],
  ];
}
function updateMax(val: columnValue | columnValue[]) {
  modelValue.value = [
    modelValue.value[0],
    (Array.isArray(val) ? val[0] : val) ?? null,
  ];
}
</script>

<template>
  <fieldset class="flex flex-col gap-2">
    <legend v-if="legend" class="sr-only">{{ legend }}</legend>
    <div class="flex items-center gap-2">
      <label
        :for="`${id}-min`"
        class="text-body-xs shrink-0 cursor-pointer text-search-filter-group-title"
      >
        {{ minLabel }}
      </label>
      <div class="min-w-0 flex-1">
        <Input
          :id="`${id}-min`"
          :type="inputType"
          :model-value="modelValue[0]"
          @update:model-value="updateMin"
        />
      </div>
    </div>
    <div class="flex items-center gap-2">
      <label
        :for="`${id}-max`"
        class="text-body-xs shrink-0 cursor-pointer text-search-filter-group-title"
      >
        {{ maxLabel }}
      </label>
      <div class="min-w-0 flex-1">
        <Input
          :id="`${id}-max`"
          :type="inputType"
          :model-value="modelValue[1]"
          @update:model-value="updateMax"
        />
      </div>
    </div>
  </fieldset>
</template>
