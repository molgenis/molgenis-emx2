<script setup lang="ts">
import { ref, computed } from "vue";
import type {
  IColumn,
  IRefColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";
import ValueRef from "../value/Ref.vue";
import ContentOntology from "../content/Ontology.vue";
import RecordListView from "./RecordListView.vue";
import Emx2ListView from "./Emx2ListView.vue";

const props = withDefaults(
  defineProps<{
    column: IColumn;
    value: any;
    showEmpty?: boolean;
    getRefClickAction?: (col: IColumn, row: IRow) => () => void;
    schemaId?: string;
    parentRowId?: Record<string, any>;
    filter?: object;
  }>(),
  {
    showEmpty: false,
  }
);

const listPage = ref(1);
const pageSize = 5;

const isOntology = computed(
  () =>
    props.column.columnType === "ONTOLOGY" ||
    props.column.columnType === "ONTOLOGY_ARRAY"
);

const isRefArray = computed(() => props.column.columnType === "REF_ARRAY");
const isRefBack = computed(() => props.column.columnType === "REFBACK");

// smart mode: use Emx2ListView when schemaId provided
const useSmartMode = computed(
  () => props.schemaId && (isRefArray.value || isRefBack.value)
);

// construct filter for refback: { refBackId: { equals: parentRowPk } }
const refbackFilter = computed(() => {
  if (!props.parentRowId || !isRefBack.value) return props.filter;
  const col = props.column as IRefColumn;
  if (!col.refBackId) return props.filter;
  return { [col.refBackId]: { equals: props.parentRowId } };
});

const allRows = computed(() => (Array.isArray(props.value) ? props.value : []));

const visibleRows = computed(() =>
  allRows.value.slice(
    (listPage.value - 1) * pageSize,
    listPage.value * pageSize
  )
);

const ontologyTree = computed(() =>
  Array.isArray(props.value) ? props.value : props.value ? [props.value] : []
);

function isEmpty(val: any): boolean {
  if (val === null || val === undefined || val === "") return true;
  if (Array.isArray(val) && val.length === 0) return true;
  return false;
}

function handleRefClick() {
  if (props.getRefClickAction) {
    props.getRefClickAction(props.column, props.value)();
  }
}
</script>

<template>
  <template v-if="isEmpty(value) && !showEmpty"></template>
  <span v-else-if="isEmpty(value) && showEmpty" class="text-gray-400 italic">
    not provided
  </span>
  <ValueRef
    v-else-if="column.columnType === 'REF'"
    :metadata="column as IRefColumn"
    :data="value"
    @refCellClicked="handleRefClick"
  />
  <!-- Smart mode: use Emx2ListView when schemaId provided -->
  <Emx2ListView
    v-else-if="useSmartMode"
    :schema-id="(column as IRefColumn).refSchemaId || schemaId!"
    :table-id="(column as IRefColumn).refTableId!"
    :filter="refbackFilter"
    :show-search="false"
    :paging-limit="5"
    :get-ref-click-action="getRefClickAction"
  />
  <!-- Dumb mode: use RecordListView when no schemaId (existing behavior) -->
  <RecordListView
    v-else-if="isRefArray || isRefBack"
    :rows="visibleRows"
    :ref-column="column as IRefColumn"
    v-model:page="listPage"
    :page-size="pageSize"
    :total-count="allRows.length"
    :get-ref-click-action="getRefClickAction"
  />
  <ContentOntology v-else-if="isOntology" :tree="ontologyTree" />
  <ValueEMX2 v-else :metadata="column" :data="value" />
</template>
