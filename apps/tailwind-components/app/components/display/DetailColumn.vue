<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import {
  isEmptyValue,
  buildRefbackFilter,
  isRefColumn,
  isRefArrayColumn,
  hasOntologyHierarchy,
  getListColumns,
} from "../../utils/displayUtils";
import { useRecordNavigation } from "../../composables/useRecordNavigation";
import ValueEMX2 from "../value/EMX2.vue";
import DataList from "./DataList.vue";
import OntologyTreeDisplay from "./OntologyTreeDisplay.vue";
import fetchMetadata from "../../composables/fetchMetadata";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    value: any;
    showEmpty?: boolean;
    schemaId?: string;
    parentRowId?: Record<string, any>;
    maxItems?: number;
  }>(),
  {
    showEmpty: false,
  }
);

const refArrayFilter = ref<Record<string, any> | undefined>();
const refTableColumns = ref<IColumn[]>([]);

watchEffect(async () => {
  const type = props.column.columnType;
  if (
    (type !== "REFBACK" && !isRefArrayColumn(type)) ||
    !props.column.refTableId ||
    !props.schemaId
  ) {
    refArrayFilter.value = undefined;
    refTableColumns.value = [];
    return;
  }
  const schema = props.column.refSchemaId || props.schemaId;
  const schemaMetadata = await fetchMetadata(schema);
  const refTable = schemaMetadata.tables.find(
    (t: ITableMetaData) => t.id === props.column.refTableId
  );
  if (!refTable?.columns) {
    refArrayFilter.value = undefined;
    refTableColumns.value = [];
    return;
  }
  const refbackCol = refTable.columns.find(
    (c) => c.columnType === "REFBACK" && c.refBackId === props.column.id
  );

  refTableColumns.value = getListColumns(refTable.columns, {
    layout: props.column.display as "TABLE" | "CARDS" | "LIST" | undefined,
  });

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

const showDataList = computed(() => {
  const type = props.column.columnType;
  if (type !== "REFBACK" && !isRefArrayColumn(type)) return false;
  if (!props.column.refTableId || !props.schemaId) return false;
  if (Array.isArray(props.value)) return true;
  if (type === "REFBACK" && props.column.refBackId && props.parentRowId)
    return true;
  if (isRefArrayColumn(type) && refArrayFilter.value) return true;
  return false;
});

const listRows = computed(() =>
  Array.isArray(props.value) ? props.value : undefined
);

const listFilter = computed(() => {
  if (props.column.columnType === "REFBACK") {
    return buildRefbackFilter(
      props.column.columnType,
      props.column.refBackId,
      props.parentRowId
    );
  }
  if (isRefArrayColumn(props.column.columnType)) {
    return refArrayFilter.value;
  }
  return undefined;
});

const isHierarchicalOntology = computed(() => {
  if (!["ONTOLOGY", "ONTOLOGY_ARRAY"].includes(props.column.columnType))
    return false;
  return hasOntologyHierarchy(props.value);
});

const { navigateToRecord } = useRecordNavigation();

const isClickableRef = computed(() => {
  return (
    !!props.schemaId &&
    !!props.column.refTableId &&
    isRefColumn(props.column.columnType) &&
    !!props.value &&
    typeof props.value === "object"
  );
});

function handleRefClick() {
  if (!props.schemaId || !props.column.refTableId || !props.value) return;
  navigateToRecord(
    props.schemaId,
    props.column.refTableId,
    props.value,
    props.column.refSchemaId
  );
}
</script>

<template>
  <template
    v-if="isEmptyValue(value) && !showEmpty && !showDataList"
  ></template>
  <span
    v-else-if="isEmptyValue(value) && showEmpty && !showDataList"
    class="text-gray-400 italic"
  >
    not provided
  </span>
  <DataList
    v-else-if="showDataList"
    :rows="listRows"
    :columns="refTableColumns"
    :schema-id="column.refSchemaId || schemaId"
    :table-id="column.refTableId"
    :filter="listFilter"
    :layout="column.display || 'TABLE'"
    :hide-columns="column.refBackId ? [column.refBackId] : undefined"
    :row-label-template="column.refLabelDefault"
  />
  <a
    v-else-if="isClickableRef"
    href="#"
    class="text-link hover:underline"
    @click.prevent="handleRefClick"
  >
    <ValueEMX2 :metadata="column" :data="value" />
  </a>
  <OntologyTreeDisplay
    v-else-if="isHierarchicalOntology"
    :value="value"
    :maxItems="maxItems"
  />
  <ValueEMX2 v-else :metadata="column" :data="value" :maxItems="maxItems" />
</template>
