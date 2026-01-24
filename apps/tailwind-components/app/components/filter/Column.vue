<script setup lang="ts">
import { computed, ref } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue, FilterOperator } from "../../../types/filters";
import Input from "../Input.vue";
import FilterRange from "./Range.vue";
import BaseIcon from "../BaseIcon.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    collapsed?: boolean;
    mobileDisplay?: boolean;
  }>(),
  {
    collapsed: true,
    mobileDisplay: false,
  }
);

const modelValue = defineModel<IFilterValue | null>();

const isCollapsed = ref(props.collapsed);

const isRangeType = computed(() =>
  ["INT", "DECIMAL", "LONG", "NON_NEGATIVE_INT", "DATE", "DATETIME"].includes(
    props.column.columnType
  )
);

const label = computed(
  () => props.column.label || props.column.id
);

const rangeValue = computed({
  get: () => (modelValue.value?.value as [any, any]) ?? [null, null],
  set: (val) => {
    if (!val[0] && !val[1]) {
      modelValue.value = null;
    } else {
      modelValue.value = { operator: "between", value: val };
    }
  },
});

const singleValue = computed({
  get: () => modelValue.value?.value ?? null,
  set: (val) => {
    if (val == null || val === "") {
      modelValue.value = null;
    } else {
      modelValue.value = { operator: getDefaultOperator(), value: val };
    }
  },
});

function getDefaultOperator(): FilterOperator {
  const type = props.column.columnType;
  if (["STRING", "TEXT", "EMAIL"].includes(type)) return "like";
  if (["BOOL"].includes(type)) return "equals";
  if (["REF", "REF_ARRAY", "ONTOLOGY", "ONTOLOGY_ARRAY"].includes(type))
    return "in";
  return "equals";
}

function handleClear() {
  modelValue.value = null;
}

function toggle() {
  isCollapsed.value = !isCollapsed.value;
}
</script>

<template>
  <hr class="mx-5 border-black opacity-10" />
  <div class="flex items-center gap-1 p-5">
    <div class="inline-flex gap-1 group" @click="toggle()">
      <h3
        class="font-sans text-body-base font-bold mr-[5px] group-hover:underline group-hover:cursor-pointer"
        :class="`text-search-filter-group-title${
          mobileDisplay ? '-mobile' : ''
        }`"
      >
        {{ label }}
      </h3>
      <span
        :class="{
          'rotate-180': isCollapsed,
          'text-search-filter-group-toggle': !mobileDisplay,
        }"
        class="flex items-center justify-center w-8 h-8 rounded-full group-hover:bg-search-filter-group-toggle group-hover:cursor-pointer"
      >
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>
    <div class="text-right grow">
      <span
        v-if="modelValue"
        class="text-body-sm hover:underline hover:cursor-pointer"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        @click="handleClear"
      >
        Clear
      </span>
    </div>
  </div>
  <div
    v-if="!isCollapsed"
    class="mb-5 ml-5 mr-5"
    :class="`text-search-filter-group-title${mobileDisplay ? '-mobile' : ''}`"
  >
    <FilterRange v-if="isRangeType" v-model="rangeValue" :id="column.id">
      <template #min="{ value, update, id }">
        <Input
          :id="id"
          :type="column.columnType"
          :model-value="value"
          @update:model-value="update"
          :ref-schema-id="column.refSchemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel"
        />
      </template>
      <template #max="{ value, update, id }">
        <Input
          :id="id"
          :type="column.columnType"
          :model-value="value"
          @update:model-value="update"
          :ref-schema-id="column.refSchemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel"
        />
      </template>
    </FilterRange>

    <Input
      v-else
      :id="column.id"
      :type="column.columnType"
      v-model="singleValue"
      :ref-schema-id="column.refSchemaId"
      :ref-table-id="column.refTableId"
      :ref-label="column.refLabel"
    />
  </div>
</template>
