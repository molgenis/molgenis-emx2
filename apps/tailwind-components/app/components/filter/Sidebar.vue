<script setup lang="ts">
import { computed, ref, toRef, useId, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";

import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { extractPrimaryKey } from "../../utils/extractPrimaryKey";
import { MAX_NESTING_DEPTH } from "../../utils/filterConstants";
import { useFilterCounts } from "../../composables/useFilterCounts";

const props = withDefaults(
  defineProps<{
    allColumns: IColumn[];
    schemaId: string;
    tableId: string;
    title?: string;
    mobileDisplay?: boolean;
    showSearch?: boolean;
  }>(),
  {
    title: "Filters",
    mobileDisplay: false,
    showSearch: false,
  }
);

const emit = defineEmits<{
  (event: "update:columns", columns: IColumn[]): void;
}>();

const searchInputId = useId();

const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];
const REF_TYPES_FOR_DEFAULT = [
  "REF",
  "REF_ARRAY",
  "SELECT",
  "RADIO",
  "CHECKBOX",
  "MULTISELECT",
  "REFBACK",
];
const MAX_DEFAULT_FILTERS = 5;

function computeDefaultFilters(columns: IColumn[]): string[] {
  const unfilterable = ["HEADING", "SECTION"];
  const filterable = columns.filter(
    (col) => !unfilterable.includes(col.columnType) && !col.id.startsWith("mg_")
  );

  const ontologyCols = filterable.filter((c) =>
    ONTOLOGY_TYPES.includes(c.columnType)
  );
  const refCols = filterable.filter((c) =>
    REF_TYPES_FOR_DEFAULT.includes(c.columnType)
  );

  const defaults = ontologyCols.slice(0, MAX_DEFAULT_FILTERS).map((c) => c.id);
  if (defaults.length < MAX_DEFAULT_FILTERS) {
    const remaining = MAX_DEFAULT_FILTERS - defaults.length;
    defaults.push(...refCols.slice(0, remaining).map((c) => c.id));
  }
  return defaults;
}

const defaultFilterIds = computed(() =>
  computeDefaultFilters(props.allColumns)
);

const route = useRoute();
const router = useRouter();

const MG_FILTERS_PARAM = "mg_filters";

function getInitialVisibleFilters(): string[] {
  const urlParam = route.query[MG_FILTERS_PARAM];
  if (typeof urlParam === "string" && urlParam.trim()) {
    return urlParam
      .split(",")
      .map((id) => id.trim())
      .filter(Boolean);
  }
  return [...defaultFilterIds.value];
}

const visibleFilterIds = ref<string[]>(getInitialVisibleFilters());

function arraysEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  const sortedA = [...a].sort();
  const sortedB = [...b].sort();
  return sortedA.every((val, idx) => val === sortedB[idx]);
}

const userHasCustomized = ref(
  typeof route.query[MG_FILTERS_PARAM] === "string"
);

watch(visibleFilterIds, (newIds) => {
  userHasCustomized.value = true;
  const isDefault = arraysEqual(newIds, defaultFilterIds.value);
  const currentQuery = { ...route.query };

  if (isDefault) {
    delete currentQuery[MG_FILTERS_PARAM];
  } else {
    currentQuery[MG_FILTERS_PARAM] = newIds.join(",");
  }

  router.replace({ query: currentQuery });
});

watch(defaultFilterIds, (newDefaults) => {
  if (userHasCustomized.value) return;
  visibleFilterIds.value = [...newDefaults];
});

function handleFilterToggle(columnId: string) {
  if (visibleFilterIds.value.includes(columnId)) {
    visibleFilterIds.value = visibleFilterIds.value.filter(
      (id) => id !== columnId
    );
    const newMap = new Map(filterStates.value);
    newMap.delete(columnId);
    filterStates.value = newMap;
  } else {
    visibleFilterIds.value = [...visibleFilterIds.value, columnId];
  }
}

function handleFilterReset() {
  const newDefaults = [...defaultFilterIds.value];
  const removedIds = visibleFilterIds.value.filter(
    (id) => !newDefaults.includes(id)
  );
  if (removedIds.length > 0) {
    const newMap = new Map(filterStates.value);
    for (const id of removedIds) {
      newMap.delete(id);
    }
    filterStates.value = newMap;
  }
  userHasCustomized.value = false;
  visibleFilterIds.value = newDefaults;
}

const filterStates = defineModel<Map<string, IFilterValue>>("filterStates", {
  default: () => new Map(),
});

const searchTerms = defineModel<string>("searchTerms", {
  default: "",
});

const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());

const { facetCounts, fetchParentCounts } = useFilterCounts({
  schemaId: toRef(props, "schemaId"),
  tableId: toRef(props, "tableId"),
  filterStates,
  columns: toRef(props, "allColumns"),
  visibleFilterIds,
  searchValue: searchTerms,
});

watch(
  visibleFilterIds,
  async (newIds) => {
    for (const id of newIds) {
      if (!id.includes(".")) continue;
      const segments = id.split(".");
      for (let depth = 0; depth < segments.length - 1; depth++) {
        const pathSoFar = segments.slice(0, depth + 1).join(".");
        if (!refColumnsCache.value.has(pathSoFar)) {
          await loadRefColumnsForPath(id);
          break;
        }
      }
    }
  },
  { immediate: true }
);

interface ResolvedFilter {
  fullPath: string;
  column: IColumn;
  schemaId: string;
  labelPrefix: string;
}

const resolvedFilters = computed<ResolvedFilter[]>(() => {
  const unfilterableTypes = ["HEADING", "SECTION"];
  const result: ResolvedFilter[] = [];

  for (const filterId of visibleFilterIds.value) {
    const segments = filterId.split(".");

    if (segments.length === 1) {
      const column = props.allColumns.find(
        (c) =>
          c.id === filterId &&
          !unfilterableTypes.includes(c.columnType) &&
          !c.id.startsWith("mg_")
      );
      if (column) {
        result.push({
          fullPath: filterId,
          column,
          schemaId: props.schemaId,
          labelPrefix: "",
        });
      }
    } else {
      const column = findColumnForPath(filterId);
      if (!column) continue;

      const labels: string[] = [];
      let currentColumns: IColumn[] = props.allColumns;
      let currentSchemaId = props.schemaId;

      for (let depth = 0; depth < segments.length - 1; depth++) {
        const seg = segments[depth]!;
        const parentCol = currentColumns.find((c) => c.id === seg);
        if (!parentCol) break;
        labels.push(parentCol.label || parentCol.id);
        currentSchemaId = parentCol.refSchemaId || currentSchemaId;
        const pathSoFar = segments.slice(0, depth + 1).join(".");
        currentColumns = refColumnsCache.value.get(pathSoFar) || [];
      }

      result.push({
        fullPath: filterId,
        column,
        schemaId: currentSchemaId,
        labelPrefix: labels.length > 0 ? labels.join(".") + "." : "",
      });
    }
  }

  return result;
});

function getFilterValue(columnId: string): IFilterValue | null {
  return filterStates.value.get(columnId) || null;
}

function findColumnForPath(fullPath: string): IColumn | undefined {
  const segments = fullPath.split(".");
  if (segments.length === 1) {
    return props.allColumns.find((c) => c.id === segments[0]);
  }

  let currentColumns: IColumn[] = props.allColumns;
  for (let depth = 0; depth < segments.length - 1; depth++) {
    const pathSoFar = segments.slice(0, depth + 1).join(".");
    const cached = refColumnsCache.value.get(pathSoFar);
    if (!cached) return undefined;
    currentColumns = cached;
  }

  return currentColumns.find((c) => c.id === segments[segments.length - 1]);
}

async function extractRefPkey(column: IColumn, val: any): Promise<any> {
  if (val === null || typeof val !== "object") return val;
  if (!column.refTableId) return val;
  const schemaId = column.refSchemaId || props.schemaId;
  try {
    const metadata = await fetchTableMetadata(schemaId, column.refTableId);
    if (Array.isArray(val)) {
      return val.map((item) =>
        typeof item === "object" && item !== null
          ? extractPrimaryKey(item, metadata)
          : item
      );
    }
    return extractPrimaryKey(val, metadata);
  } catch {
    return val;
  }
}

async function setFilterValue(
  columnId: string,
  value: IFilterValue | null | undefined
) {
  const newMap = new Map(filterStates.value);
  if (value === null || value === undefined) {
    newMap.delete(columnId);
  } else {
    const column = findColumnForPath(columnId);
    if (column && column.refTableId && value.value !== null) {
      const stripped = await extractRefPkey(column, value.value);
      newMap.set(columnId, { ...value, value: stripped });
    } else {
      newMap.set(columnId, value);
    }
  }
  filterStates.value = newMap;
}

async function loadRefColumnsForPath(fullPath: string) {
  const segments = fullPath.split(".");
  let currentColumns: IColumn[] = props.allColumns;
  let currentSchemaId = props.schemaId;

  for (
    let depth = 0;
    depth < segments.length && depth < MAX_NESTING_DEPTH;
    depth++
  ) {
    const segment = segments[depth]!;
    const pathSoFar = segments.slice(0, depth + 1).join(".");

    const column = currentColumns.find((c) => c.id === segment);
    if (!column || !column.refTableId) return;

    if (!refColumnsCache.value.has(pathSoFar)) {
      const refSchemaId = column.refSchemaId || currentSchemaId;
      try {
        const tableMetadata = await fetchTableMetadata(
          refSchemaId,
          column.refTableId
        );
        const unfilterable = ["HEADING", "SECTION", "REFBACK"];
        refColumnsCache.value.set(
          pathSoFar,
          tableMetadata.columns.filter(
            (col) =>
              !col.id.startsWith("mg_") &&
              !unfilterable.includes(col.columnType) &&
              col.showFilter !== false
          )
        );
      } catch {
        return;
      }
    }

    currentColumns = refColumnsCache.value.get(pathSoFar) || [];
    currentSchemaId = column.refSchemaId || currentSchemaId;
  }
}
</script>

<template>
  <div
    class="rounded-t-3px rounded-b-50px pb-8"
    :class="{ 'bg-sidebar-gradient': !mobileDisplay }"
  >
    <div v-if="!mobileDisplay" class="p-5">
      <h2
        class="uppercase font-display text-heading-3xl text-search-filter-title"
      >
        {{ title }}
      </h2>
    </div>
    <div class="px-5 pb-3 flex justify-end">
      <FilterPicker
        :columns="allColumns"
        :visible-filter-ids="visibleFilterIds"
        :default-filter-ids="defaultFilterIds"
        :schema-id="schemaId"
        @toggle="handleFilterToggle"
        @reset="handleFilterReset"
      />
    </div>

    <div v-if="showSearch" class="px-5 pb-5">
      <InputSearch
        :id="searchInputId"
        v-model="searchTerms"
        placeholder="Search..."
      />
    </div>

    <FilterColumn
      v-for="filter in resolvedFilters"
      :key="filter.fullPath"
      :column="filter.column"
      :schema-id="filter.schemaId"
      :label-prefix="filter.labelPrefix"
      :model-value="getFilterValue(filter.fullPath)"
      @update:model-value="setFilterValue(filter.fullPath, $event)"
      :mobile-display="mobileDisplay"
      :depth="0"
      :removable="true"
      @remove="handleFilterToggle(filter.fullPath)"
      :facet-counts="facetCounts.get(filter.column.id)"
      :fetch-parent-counts="fetchParentCounts"
    />
  </div>
</template>
