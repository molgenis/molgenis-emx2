<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import type { IColumnDisplay } from "../../../types/types";
import {
  isEmptyValue,
  buildRefbackFilter,
  isRefColumn,
  isRefArrayColumn,
  buildRefHref,
} from "../../utils/displayUtils";
import { getPrimaryKey } from "../../utils/getPrimaryKey";
import ValueEMX2 from "../value/EMX2.vue";
import Emx2ListView from "./Emx2ListView.vue";
import fetchMetadata from "../../composables/fetchMetadata";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

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

const refArrayFilter = ref<Record<string, any> | undefined>();

watchEffect(async () => {
  if (
    !isRefArrayColumn(props.column.columnType) ||
    !props.column.refTableId ||
    !props.schemaId ||
    !props.parentRowId
  ) {
    refArrayFilter.value = undefined;
    return;
  }
  const schema = props.column.refSchemaId || props.schemaId;
  const schemaMetadata = await fetchMetadata(schema);
  const refTable = schemaMetadata.tables.find(
    (t: ITableMetaData) => t.id === props.column.refTableId
  );
  if (!refTable?.columns) {
    refArrayFilter.value = undefined;
    return;
  }
  const refbackCol = refTable.columns.find(
    (c) => c.columnType === "REFBACK" && c.refBackId === props.column.id
  );
  if (refbackCol && props.parentRowId) {
    const keyFilter: Record<string, any> = {};
    for (const [key, val] of Object.entries(props.parentRowId)) {
      keyFilter[key] = { equals: val };
    }
    refArrayFilter.value = { [refbackCol.id]: keyFilter };
  } else {
    refArrayFilter.value = undefined;
  }
});

const showListView = computed(() => {
  if (!props.column.refTableId || !props.schemaId) return false;
  const type = props.column.columnType;
  if (type === "REFBACK" && props.column.refBackId && props.parentRowId) {
    return true;
  }
  if (
    isRefArrayColumn(type) &&
    props.column.listConfig &&
    refArrayFilter.value
  ) {
    return true;
  }
  return false;
});

const listFilter = computed(() => {
  if (props.column.columnType === "REFBACK") {
    return (
      buildRefbackFilter(
        props.column.columnType,
        props.column.refBackId,
        props.parentRowId
      ) || props.column.listConfig?.filter
    );
  }
  if (isRefArrayColumn(props.column.columnType)) {
    return refArrayFilter.value || props.column.listConfig?.filter;
  }
  return props.column.listConfig?.filter;
});

const refHref = ref<string | undefined>();

watchEffect(async () => {
  if (
    !props.schemaId ||
    !props.column.refTableId ||
    !isRefColumn(props.column.columnType) ||
    !props.value ||
    typeof props.value !== "object"
  ) {
    refHref.value = undefined;
    return;
  }
  const schema = props.column.refSchemaId || props.schemaId;
  const rowKey = await getPrimaryKey(
    props.value,
    props.column.refTableId,
    schema
  );
  refHref.value = buildRefHref(
    props.schemaId,
    props.column.refTableId,
    props.column.refSchemaId,
    rowKey
  );
});
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
  <NuxtLink v-else-if="refHref" :to="refHref" class="text-link hover:underline">
    <ValueEMX2 :metadata="column" :data="value" />
  </NuxtLink>
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
