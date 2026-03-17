<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";
import type { ActiveFilter } from "../../../types/filters";

import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { getPrimaryKey } from "../../utils/getPrimaryKey";
import { MAX_NESTING_DEPTH } from "../../utils/filterConstants";
import { buildGraphQLFilter } from "../../utils/buildFilter";
import { computeDefaultFilters } from "../../utils/computeDefaultFilters";
import { formatFilterValue } from "../../utils/formatFilterValue";
import type { IGraphQLFilter } from "../../../types/filters";
import { createCountFetcher, type ICountFetcher } from "../../utils/createCountFetcher";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    title?: string;
    showSearch?: boolean;
  }>(),
  {
    title: "Filters",
    showSearch: false,
  }
);

const searchInputId = useId();

const filterableColumns = ref<IColumn[]>([]);

const unfilterableTypes = ["HEADING", "SECTION", "FILE", "REFBACK"];

watch(
  () => [props.schemaId, props.tableId] as const,
  async ([newSchemaId, newTableId]) => {
    if (!newSchemaId || !newTableId) {
      filterableColumns.value = [];
      return;
    }
    try {
      const tableMetadata = await fetchTableMetadata(newSchemaId, newTableId);
      filterableColumns.value = tableMetadata.columns
        .filter(
          (c) =>
            !c.id.startsWith("mg_") && !unfilterableTypes.includes(c.columnType)
        )
        .map((c) => ({ ...c, refSchemaId: c.refSchemaId || newSchemaId }));
    } catch {
      filterableColumns.value = [];
    }
  },
  { immediate: true }
);

const defaultFilterIds = computed(() =>
  computeDefaultFilters(filterableColumns.value)
);

const route = useRoute();
const router = useRouter();

const MG_FILTERS_PARAM = "mg_filters";
const MAX_VISIBLE_FILTERS = 25;

function getInitialVisibleFilters(): string[] {
  const urlParam = route.query[MG_FILTERS_PARAM];
  if (typeof urlParam === "string" && urlParam.trim()) {
    return urlParam
      .split(",")
      .map((id) => id.trim())
      .filter(Boolean)
      .slice(0, MAX_VISIBLE_FILTERS);
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
  } else if (visibleFilterIds.value.length < MAX_VISIBLE_FILTERS) {
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

const crossFilterMap = computed(() => {
  const map = new Map<string, IGraphQLFilter>();
  for (const filterId of visibleFilterIds.value) {
    const crossFilterStates = new Map<string, IFilterValue>();
    filterStates.value.forEach((value, key) => {
      if (key !== filterId) {
        crossFilterStates.set(key, value);
      }
    });
    map.set(filterId, buildGraphQLFilter(crossFilterStates, filterableColumns.value, searchTerms.value));
  }
  return map;
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

interface IFilter {
  fullPath: string;
  column: IColumn;
  label: string;
}

const resolvedFilters = computed<IFilter[]>(() => {
  const result: IFilter[] = [];

  for (const filterId of visibleFilterIds.value) {
    const segments = filterId.split(".");

    if (segments.length === 1) {
      const column = filterableColumns.value.find((c) => c.id === filterId);
      if (column) {
        result.push({
          fullPath: filterId,
          column,
          label: column.label || column.id,
        });
      }
    } else {
      const column = findColumnForPath(filterId);
      if (!column) continue;

      const labels: string[] = [];
      let currentColumns: IColumn[] = filterableColumns.value;

      for (let depth = 0; depth < segments.length - 1; depth++) {
        const seg = segments[depth]!;
        const parentCol = currentColumns.find((c) => c.id === seg);
        if (!parentCol) break;
        labels.push(parentCol.label || parentCol.id);
        const pathSoFar = segments.slice(0, depth + 1).join(".");
        currentColumns = refColumnsCache.value.get(pathSoFar) || [];
      }

      result.push({
        fullPath: filterId,
        column,
        label: labels.join(".") + "." + (column.label || column.id),
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
    return filterableColumns.value.find((c) => c.id === segments[0]);
  }

  let currentColumns: IColumn[] = filterableColumns.value;
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
    if (Array.isArray(val)) {
      return await Promise.all(
        val.map((item) =>
          typeof item === "object" && item !== null
            ? getPrimaryKey(item, column.refTableId!, schemaId)
            : item
        )
      );
    }
    return await getPrimaryKey(val, column.refTableId, schemaId);
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

const activeFiltersList = computed<ActiveFilter[]>(() => {
  const result: ActiveFilter[] = [];
  for (const [columnId, filterValue] of filterStates.value) {
    const filter = resolvedFilters.value.find((f) => f.fullPath === columnId);
    const label = filter?.label || columnId;
    const { displayValue, values } = formatFilterValue(filterValue);
    if (displayValue) {
      result.push({ columnId, label, displayValue, values });
    }
  }
  return result;
});

function getCountFetcher(columnPath: string): ICountFetcher {
  return createCountFetcher({
    schemaId: props.schemaId,
    tableId: props.tableId,
    columnPath,
    getCrossFilter: () => crossFilterMap.value.get(columnPath),
  });
}

async function loadRefColumnsForPath(fullPath: string) {
  const segments = fullPath.split(".");
  let currentColumns: IColumn[] = filterableColumns.value;
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
              (col as any).showFilter !== false
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
  <div class="filter-sidebar-context rounded-t-3px rounded-b-50px pb-8 bg-sidebar-gradient">
    <div class="p-5">
      <h2
        class="uppercase font-display text-heading-3xl text-search-filter-title"
      >
        {{ title }}
      </h2>
    </div>
    <div class="px-5 pb-3 flex justify-end">
      <FilterPicker
        :columns="filterableColumns"
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
      :label="filter.label"
      :model-value="getFilterValue(filter.fullPath)"
      @update:model-value="setFilterValue(filter.fullPath, $event)"
      :removable="true"
      @remove="handleFilterToggle(filter.fullPath)"
      :count-fetcher="getCountFetcher(filter.fullPath)"
    />
  </div>
</template>

<style scoped>
.filter-sidebar-context {
  --text-color-title-contrast: var(--text-color-search-filter-group-title);
  --text-color-input-description: var(--text-color-search-filter-group-title);
  --text-color-input: var(--text-color-search-filter-group-title);
}
</style>
