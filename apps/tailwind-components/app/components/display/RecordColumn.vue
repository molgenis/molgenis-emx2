<script setup lang="ts">
import { computed } from "vue";
import type { IColumnDisplay } from "../../../types/types";
import { isEmptyValue, buildRefbackFilter } from "../../utils/displayUtils";
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

const showListView = computed(() => {
  if (!props.column.refTableId || !props.schemaId) return false;
  const type = props.column.columnType;
  if (type === "REFBACK" && props.column.refBackId && props.parentRowId) {
    return true;
  }
  return false;
});

const listFilter = computed(
  () =>
    buildRefbackFilter(
      props.column.columnType,
      props.column.refBackId,
      props.parentRowId
    ) || props.column.listConfig?.filter
);
</script>

<template>
  <template
    v-if="isEmptyValue(value) && !showEmpty && !showListView"
  ></template>
  <span
    v-else-if="isEmptyValue(value) && showEmpty && !showListView"
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
