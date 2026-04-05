<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type {
  IFilterValue,
  FilterOperator,
  FilterValue,
} from "../../../types/filters";
import type { ICountFetcher } from "../../utils/createCountFetcher";
import Input from "../Input.vue";
import FilterRange from "./Range.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    label?: string;
    removable?: boolean;
    countFetcher?: ICountFetcher;
    showLabel?: boolean;
  }>(),
  {
    removable: false,
    showLabel: true,
  }
);

const effectiveRefSchemaId = computed(() => props.column.refSchemaId ?? "");

const emit = defineEmits<{
  (event: "remove"): void;
}>();

const modelValue = defineModel<IFilterValue | null>();

const isRangeType = computed(() =>
  [
    "INT",
    "DECIMAL",
    "LONG",
    "NON_NEGATIVE_INT",
    "DATE",
    "DATETIME",
    "INT_ARRAY",
    "DECIMAL_ARRAY",
    "LONG_ARRAY",
    "DATE_ARRAY",
    "NON_NEGATIVE_INT_ARRAY",
    "DATETIME_ARRAY",
  ].includes(props.column.columnType)
);

const REF_FILTER_TYPES = [
  "REF",
  "REF_ARRAY",
  "REFBACK",
  "SELECT",
  "MULTISELECT",
  "RADIO",
  "CHECKBOX",
];

const ONTOLOGY_FILTER_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];

const filterType = computed((): CellValueType => {
  const type = props.column.columnType;
  if (REF_FILTER_TYPES.includes(type)) return "REF_ARRAY";
  if (ONTOLOGY_FILTER_TYPES.includes(type)) return "ONTOLOGY_ARRAY";
  const STRING_FILTER_TYPES = [
    "STRING",
    "TEXT",
    "EMAIL",
    "HYPERLINK",
    "AUTO_ID",
    "JSON",
    "STRING_ARRAY",
    "TEXT_ARRAY",
    "EMAIL_ARRAY",
    "HYPERLINK_ARRAY",
    "UUID_ARRAY",
    "PERIOD",
    "PERIOD_ARRAY",
  ];
  if (STRING_FILTER_TYPES.includes(type)) return "STRING";
  if (type === "BOOL_ARRAY") return "BOOL";
  const RANGE_ARRAY_MAP: Record<string, CellValueType> = {
    INT_ARRAY: "INT",
    DECIMAL_ARRAY: "DECIMAL",
    LONG_ARRAY: "LONG",
    DATE_ARRAY: "DATE",
    NON_NEGATIVE_INT_ARRAY: "NON_NEGATIVE_INT",
    DATETIME_ARRAY: "DATETIME",
  };
  if (RANGE_ARRAY_MAP[type]) return RANGE_ARRAY_MAP[type];
  return type as CellValueType;
});

const label = computed(
  () =>
    props.label ??
    ((props.column as any).displayConfig?.label ||
      props.column.label ||
      props.column.id)
);

const rangeValue = computed({
  get: () => (modelValue.value?.value as [any, any]) ?? [null, null],
  set: (val) => {
    if (val[0] == null && val[1] == null) {
      modelValue.value = null;
    } else {
      modelValue.value = { operator: "between", value: val };
    }
  },
});

const singleValue = computed({
  get: (): columnValue | columnValue[] => {
    if (!modelValue.value) return null;
    return (modelValue.value.value ?? null) as columnValue | columnValue[];
  },
  set: (val) => {
    if (val == null || val === "") {
      modelValue.value = null;
    } else {
      modelValue.value = {
        operator: getDefaultOperator(),
        value: val as FilterValue,
      };
    }
  },
});

function getDefaultOperator(): FilterOperator {
  const type = props.column.columnType;
  if (["STRING", "TEXT", "EMAIL", "HYPERLINK"].includes(type)) return "like";
  return "equals";
}

function handleClear() {
  modelValue.value = null;
}
</script>

<template>
  <template v-if="showLabel">
    <hr
      class="mx-5 opacity-20"
      style="border-color: var(--text-color-search-filter-group-title)"
    />
    <div class="flex items-center gap-0 px-5">
      <h3
        class="font-sans text-body-base font-bold text-search-filter-group-title"
      >
        {{ label }}
      </h3>
      <div
        v-if="removable"
        class="text-right grow flex gap-2 items-center justify-end"
      >
        <span
          class="text-body-sm hover:underline hover:cursor-pointer text-search-filter-expand"
          @click="emit('remove')"
        >
          Remove
        </span>
      </div>
    </div>
  </template>
  <div class="mb-5 ml-5 mr-5 overflow-hidden text-search-filter-group-title">
    <FilterRange v-if="isRangeType" v-model="rangeValue" :id="column.id">
      <template #min="{ value, update, id }">
        <Input
          :id="id"
          :type="filterType"
          :model-value="value"
          @update:model-value="update"
          :ref-schema-id="effectiveRefSchemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel || column.refLabelDefault"
        />
      </template>
      <template #max="{ value, update, id }">
        <Input
          :id="id"
          :type="filterType"
          :model-value="value"
          @update:model-value="update"
          :ref-schema-id="effectiveRefSchemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel || column.refLabelDefault"
        />
      </template>
    </FilterRange>

    <Input
      v-else
      :id="column.id"
      :type="filterType"
      v-model="singleValue"
      :ref-schema-id="effectiveRefSchemaId"
      :ref-table-id="column.refTableId"
      :ref-label="column.refLabel || column.refLabelDefault"
      :show-clear="false"
      :count-fetcher="countFetcher"
      :force-list="true"
    />
    <span
      v-if="modelValue"
      class="mt-1 inline-block text-body-sm hover:underline hover:cursor-pointer text-search-filter-expand"
      @click="handleClear"
    >
      Clear
    </span>
  </div>
</template>
