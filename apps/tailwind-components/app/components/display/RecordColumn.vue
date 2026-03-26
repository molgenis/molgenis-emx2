<script setup lang="ts">
import { computed } from "vue";
import type { IColumnDisplay } from "../../../types/types";
import ValueEMX2 from "../value/EMX2.vue";
import Emx2ListView from "./Emx2ListView.vue";

const props = withDefaults(
  defineProps<{
    column: IColumnDisplay;
    value: any;
    showEmpty?: boolean;
    schemaId?: string;
    parentRowId?: Record<string, any>;
  }>(),
  {
    showEmpty: false,
  }
);

function isEmpty(val: any): boolean {
  if (val === null || val === undefined || val === "") return true;
  if (Array.isArray(val) && val.length === 0) return true;
  return false;
}

const showListView = computed(() => {
  if (!props.column.refTableId || !props.schemaId) return false;
  const type = props.column.columnType;
  if (type === "REFBACK" && props.column.refBackId && props.parentRowId) {
    return true;
  }
  return false;
});

const listFilter = computed(() => {
  if (
    props.column.columnType === "REFBACK" &&
    props.column.refBackId &&
    props.parentRowId
  ) {
    const keyFilter: Record<string, any> = {};
    for (const [key, val] of Object.entries(props.parentRowId)) {
      keyFilter[key] = { equals: val };
    }
    return { [props.column.refBackId]: keyFilter };
  }
  return props.column.listConfig?.filter;
});
</script>

<template>
  <template v-if="isEmpty(value) && !showEmpty && !showListView"></template>
  <span
    v-else-if="isEmpty(value) && showEmpty && !showListView"
    class="text-gray-400 italic"
  >
    not provided
  </span>
  <component
    v-else-if="column.displayComponent"
    :is="column.displayComponent"
    :column="column"
    :value="value"
    :show-empty="showEmpty"
  />
  <Emx2ListView
    v-else-if="showListView"
    :schema-id="column.refSchemaId || schemaId!"
    :table-id="column.refTableId!"
    :filter="listFilter"
    :config="column.listConfig"
  />
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
