<script setup lang="ts">
import { ref, watch } from "vue";
import type {
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import { nonEmptyRanges } from "../../utils/filterUtils";
import Button from "../Button.vue";
import Input from "../Input.vue";

type Range = [columnValue, columnValue];

const props = withDefaults(
  defineProps<{
    id: string;
    inputType: CellValueType;
    modelValue: Range[];
    legend?: string;
    minLabel?: string;
    maxLabel?: string;
  }>(),
  {
    minLabel: "Min",
    maxLabel: "Max",
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: Range[]];
}>();

function rowsFrom(ranges: Range[]): Range[] {
  return ranges.length > 0
    ? ranges.map(([min, max]) => [min, max])
    : [[null, null]];
}

const rows = ref<Range[]>(rowsFrom(props.modelValue));

watch(
  () => props.modelValue,
  (incoming) => {
    const shown = JSON.stringify(nonEmptyRanges(rows.value));
    if (shown !== JSON.stringify(nonEmptyRanges(incoming))) {
      rows.value = rowsFrom(incoming);
    }
  }
);

function commit() {
  emit(
    "update:modelValue",
    rows.value.map(([min, max]) => [min, max])
  );
}

function setBound(
  index: number,
  side: 0 | 1,
  val: columnValue | columnValue[]
) {
  rows.value[index]![side] = (Array.isArray(val) ? val[0] : val) ?? null;
  commit();
}

function addRow() {
  rows.value.push([null, null]);
  commit();
}

function removeRow(index: number) {
  rows.value.splice(index, 1);
  commit();
}
</script>

<template>
  <fieldset class="flex flex-col gap-2">
    <legend v-if="legend" class="sr-only">{{ legend }}</legend>
    <div
      v-for="(row, index) in rows"
      :key="index"
      class="flex items-center gap-1"
    >
      <div class="flex flex-1 flex-col gap-2">
        <div class="flex items-center gap-2">
          <label
            :for="`${id}-${index}-min`"
            class="text-body-xs shrink-0 cursor-pointer text-search-filter-group-title"
          >
            {{ minLabel }}
          </label>
          <div class="min-w-0 flex-1">
            <Input
              :id="`${id}-${index}-min`"
              :type="inputType"
              :model-value="row[0]"
              @update:model-value="setBound(index, 0, $event)"
            />
          </div>
        </div>
        <div class="flex items-center gap-2">
          <label
            :for="`${id}-${index}-max`"
            class="text-body-xs shrink-0 cursor-pointer text-search-filter-group-title"
          >
            {{ maxLabel }}
          </label>
          <div class="min-w-0 flex-1">
            <Input
              :id="`${id}-${index}-max`"
              :type="inputType"
              :model-value="row[1]"
              @update:model-value="setBound(index, 1, $event)"
            />
          </div>
        </div>
      </div>
      <Button
        v-if="rows.length > 1"
        iconOnly
        type="secondary"
        icon="trash"
        label="Remove item"
        @click="removeRow(index)"
      />
    </div>
    <Button
      type="text"
      size="small"
      icon="plus"
      label="Add an additional item"
      @click="addRow"
    />
  </fieldset>
</template>
