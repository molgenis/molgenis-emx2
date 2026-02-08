<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue, FilterOperator } from "../../../types/filters";
import Input from "../Input.vue";
import FilterRange from "./Range.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    schemaId?: string;
    mobileDisplay?: boolean;
    depth?: number;
    labelPrefix?: string;
    removable?: boolean;
  }>(),
  {
    schemaId: "",
    mobileDisplay: false,
    depth: 0,
    labelPrefix: "",
    removable: false,
  }
);

const effectiveRefSchemaId = computed(
  () => props.column.refSchemaId || props.schemaId
);

const emit = defineEmits<{
  (event: "remove"): void;
}>();

const modelValue = defineModel<IFilterValue | null>();

const isRangeType = computed(() =>
  ["INT", "DECIMAL", "LONG", "NON_NEGATIVE_INT", "DATE", "DATETIME"].includes(
    props.column.columnType
  )
);

const refTypes = ["REF", "REF_ARRAY", "ONTOLOGY", "ONTOLOGY_ARRAY"];

const isRefType = computed(() => refTypes.includes(props.column.columnType));

const label = computed(
  () =>
    props.labelPrefix +
    (props.column.displayConfig?.label || props.column.label || props.column.id)
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
  if (["STRING", "TEXT", "EMAIL", "FILE"].includes(type)) return "like";
  if (["BOOL"].includes(type)) return "equals";
  const refTypes = [
    "REF",
    "REF_ARRAY",
    "SELECT",
    "MULTISELECT",
    "RADIO",
    "CHECKBOX",
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
  ];
  if (refTypes.includes(type) || props.column.refTableId) return "equals";
  return "equals";
}

function handleClear() {
  modelValue.value = null;
}
</script>

<template>
  <hr class="mx-5 border-black opacity-10" />
  <div class="flex items-center gap-1 px-5 pt-5 pb-2">
    <h3
      class="font-sans text-body-base font-bold"
      :class="`text-search-filter-group-title${mobileDisplay ? '-mobile' : ''}`"
    >
      {{ label }}
    </h3>
    <div class="text-right grow flex gap-2 items-center justify-end">
      <span
        v-if="modelValue"
        class="text-body-sm hover:underline hover:cursor-pointer"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        @click="handleClear"
      >
        Clear
      </span>
      <span
        v-if="removable"
        class="text-body-sm hover:underline hover:cursor-pointer"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        @click="emit('remove')"
      >
        Remove
      </span>
    </div>
  </div>
  <div
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
          :ref-schema-id="effectiveRefSchemaId"
          :ref-table-id="column.refTableId"
          :ref-label="column.refLabel || column.refLabelDefault"
        />
      </template>
      <template #max="{ value, update, id }">
        <Input
          :id="id"
          :type="column.columnType"
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
      :type="column.columnType"
      v-model="singleValue"
      :ref-schema-id="effectiveRefSchemaId"
      :ref-table-id="column.refTableId"
      :ref-label="column.refLabel || column.refLabelDefault"
      :show-clear="!isRefType"
    />
  </div>
</template>
