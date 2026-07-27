<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import {
  isOntologyType,
  isPartsType,
  isRefArrayType,
  isRefbackType,
  isSingleRefType,
} from "../../../../metadata-utils/src";
import {
  isEmptyValue,
  buildRefbackFilter,
  isDataListColumn,
  hasOntologyHierarchy,
  getListColumns,
} from "../../utils/displayUtils";
import { buildContextualListPath } from "../../utils/recordPath";
import { useRecordNavigation } from "../../composables/useRecordNavigation";
import BaseIcon from "../BaseIcon.vue";
import ValueEMX2 from "../value/EMX2.vue";
import DataList from "./DataList.vue";
import OntologyTreeDisplay from "./OntologyTreeDisplay.vue";
import fetchMetadata from "../../composables/fetchMetadata";
import fetchOntologyAncestry from "../../composables/fetchOntologyAncestry";
import { resolveOntologyAncestry } from "../../utils/resolveOntologyAncestry";
import type { IOntologyTreeItem } from "../../utils/buildOntologyTree";
import type {
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";

const emit = defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const props = withDefaults(
  defineProps<{
    column: IColumn;
    value: any;
    count?: number;
    showEmpty?: boolean;
    schemaId?: string;
    parentTableId?: string;
    parentRowId?: Record<string, any>;
    maxItems?: number;
  }>(),
  {
    showEmpty: false,
  }
);

const refArrayFilter = ref<Record<string, any> | undefined>();
const refTableColumns = ref<IColumn[]>([]);
const refSchemaMetadata = ref<ISchemaMetaData>();

watchEffect(async () => {
  if (!isDataListColumn(props.column) || !props.schemaId) {
    refArrayFilter.value = undefined;
    refTableColumns.value = [];
    refSchemaMetadata.value = undefined;
    return;
  }
  const schema = props.column.refSchemaId || props.schemaId;
  const schemaMetadata = await fetchMetadata(schema);
  refSchemaMetadata.value = schemaMetadata;
  const refTable = schemaMetadata.tables.find(
    (t: ITableMetaData) => t.id === props.column.refTableId
  );
  if (!refTable?.columns) {
    refArrayFilter.value = undefined;
    refTableColumns.value = [];
    return;
  }
  const refbackCol = refTable.columns.find(
    (c) => isRefbackType(c.columnType) && c.refBackId === props.column.id
  );

  refTableColumns.value = getListColumns(refTable.columns, {
    layout: props.column.display,
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
  if (!isDataListColumn(props.column) || !props.schemaId) return false;
  if (Array.isArray(props.value)) return true;
  if (isRefbackType(type) && props.column.refBackId && props.parentRowId)
    return true;
  if (isRefArrayType(type) && refArrayFilter.value) return true;
  return false;
});

const contextualListPath = computed(() => {
  if (
    !refSchemaMetadata.value ||
    !props.parentTableId ||
    !props.parentRowId ||
    !isPartsType(props.column.columnType)
  )
    return null;
  return buildContextualListPath(
    refSchemaMetadata.value,
    props.parentTableId,
    props.parentRowId,
    props.column.id
  );
});

const listRows = computed(() =>
  Array.isArray(props.value) ? props.value : undefined
);

const listFilter = computed(() => {
  if (isRefbackType(props.column.columnType)) {
    return buildRefbackFilter(
      props.column.columnType,
      props.column.refBackId,
      props.parentRowId
    );
  }
  if (isRefArrayType(props.column.columnType)) {
    return refArrayFilter.value;
  }
  return undefined;
});

const ontologyTerms = ref<IOntologyTreeItem[]>([]);

watchEffect(async () => {
  const terms = isOntologyType(props.column.columnType)
    ? (props.value as IOntologyTreeItem[] | null)
    : null;
  if (!Array.isArray(terms)) {
    ontologyTerms.value = [];
    return;
  }
  ontologyTerms.value = terms;

  const ontologySchemaId = props.column.refSchemaId || props.schemaId;
  const ontologyTableId = props.column.refTableId;
  if (!ontologySchemaId || !ontologyTableId) return;

  try {
    const termsByName = await fetchOntologyAncestry(
      ontologySchemaId,
      ontologyTableId,
      terms.map((term) => term.name)
    );
    ontologyTerms.value = resolveOntologyAncestry(terms, termsByName);
  } catch (error) {
    console.error(
      `Could not resolve ontology ancestry for ${ontologySchemaId}.${ontologyTableId}`,
      error
    );
  }
});

const isHierarchicalOntology = computed(() =>
  hasOntologyHierarchy(ontologyTerms.value)
);

const { navigateToRecord } = useRecordNavigation();

const isClickableRef = computed(() => {
  return (
    !!props.schemaId &&
    !!props.column.refTableId &&
    isSingleRefType(props.column.columnType) &&
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
    class="text-disabled italic"
  >
    not provided
  </span>
  <div v-else-if="showDataList">
    <DataList
      :rows="listRows"
      :total-count="count"
      :columns="refTableColumns"
      :schema-id="column.refSchemaId || schemaId"
      :table-id="column.refTableId"
      :filter="listFilter"
      :layout="column.display || 'TABLE'"
      :hide-columns="column.refBackId ? [column.refBackId] : undefined"
      :row-label-template="column.refLabelDefault"
      @valueClick="emit('valueClick', $event)"
    />
    <NuxtLink
      v-if="contextualListPath"
      :to="contextualListPath"
      class="inline-flex items-center gap-1 mt-2 text-link hover:underline"
      :aria-label="`View all ${column.label || column.id}`"
    >
      View all
      <BaseIcon name="arrow-right" :width="16" />
    </NuxtLink>
  </div>
  <a
    v-else-if="isClickableRef"
    href="#"
    class="text-link hover:underline"
    @click.prevent="handleRefClick"
  >
    <ValueEMX2
      :metadata="column"
      :data="value"
      @valueClick="emit('valueClick', $event)"
    />
  </a>
  <OntologyTreeDisplay
    v-else-if="isHierarchicalOntology"
    :value="ontologyTerms"
    :maxItems="maxItems"
  />
  <ValueEMX2
    v-else
    :metadata="column"
    :data="value"
    :maxItems="maxItems"
    @valueClick="emit('valueClick', $event)"
  />
</template>
