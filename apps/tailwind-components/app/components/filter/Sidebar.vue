<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";

import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { extractPrimaryKey } from "../../utils/extractPrimaryKey";

const props = withDefaults(
  defineProps<{
    allColumns: IColumn[];
    schemaId: string;
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

watch(visibleFilterIds, (newIds) => {
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
  const urlParam = route.query[MG_FILTERS_PARAM];
  if (!urlParam || (typeof urlParam === "string" && !urlParam.trim())) {
    visibleFilterIds.value = [...newDefaults];
  }
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
  visibleFilterIds.value = newDefaults;
}

const directFilterIds = computed(() => {
  return new Set(visibleFilterIds.value.filter((id) => !id.includes(".")));
});

const nestedParentIds = computed(() => {
  const parents = new Set<string>();
  for (const id of visibleFilterIds.value) {
    if (id.includes(".")) {
      parents.add(id.split(".")[0]!);
    }
  }
  return parents;
});

const filterableColumnsComputed = computed<IColumn[]>(() => {
  const unfilterableTypes = ["HEADING", "SECTION"];
  return props.allColumns.filter(
    (col) =>
      !unfilterableTypes.includes(col.columnType) &&
      !col.id.startsWith("mg_") &&
      directFilterIds.value.has(col.id)
  );
});

const nestedOnlyParents = computed<IColumn[]>(() => {
  const unfilterableTypes = ["HEADING", "SECTION"];
  return props.allColumns.filter(
    (col) =>
      !unfilterableTypes.includes(col.columnType) &&
      !col.id.startsWith("mg_") &&
      nestedParentIds.value.has(col.id) &&
      !directFilterIds.value.has(col.id)
  );
});

const filterStates = defineModel<Map<string, IFilterValue>>("filterStates", {
  default: () => new Map(),
});

const searchTerms = defineModel<string>("searchTerms", {
  default: "",
});

const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());

const nestedFilterIds = computed(() => {
  const nested = new Map<string, string[]>();
  for (const id of visibleFilterIds.value) {
    if (id.includes(".")) {
      const parentId = id.split(".")[0]!;
      const childId = id.split(".")[1]!;
      if (!nested.has(parentId)) nested.set(parentId, []);
      nested.get(parentId)!.push(childId);
    }
  }
  return nested;
});

watch(
  nestedFilterIds,
  async (newNested) => {
    for (const parentId of newNested.keys()) {
      if (!refColumnsCache.value.has(parentId)) {
        const parentCol = props.allColumns.find((c) => c.id === parentId);
        if (parentCol) await loadRefColumns(parentCol);
      }
    }
  },
  { immediate: true }
);

function visibleNestedColumns(parentId: string): IColumn[] {
  const all = refColumnsCache.value.get(parentId) || [];
  const selectedChildIds = nestedFilterIds.value.get(parentId);
  if (!selectedChildIds) return [];
  return all.filter((c) => selectedChildIds.includes(c.id));
}

function getFilterValue(columnId: string): IFilterValue | null {
  return filterStates.value.get(columnId) || null;
}

function findColumnForId(columnId: string): IColumn | undefined {
  const segments = columnId.split(".");
  if (segments.length === 1) {
    return props.allColumns.find((c) => c.id === segments[0]);
  }
  const nested = refColumnsCache.value.get(segments[0]!);
  return nested?.find((c) => c.id === segments[1]);
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
    const column = findColumnForId(columnId);
    if (column && column.refTableId && value.value !== null) {
      const stripped = await extractRefPkey(column, value.value);
      newMap.set(columnId, { ...value, value: stripped });
    } else {
      newMap.set(columnId, value);
    }
  }
  filterStates.value = newMap;
}

async function loadRefColumns(column: IColumn) {
  const key = column.id;
  if (refColumnsCache.value.has(key)) return;

  const refSchemaId = column.refSchemaId || props.schemaId;
  const refTableId = column.refTableId;
  if (!refTableId) return;

  try {
    const tableMetadata = await fetchTableMetadata(refSchemaId, refTableId);
    const unfilterable = ["HEADING", "SECTION", "REFBACK"];
    refColumnsCache.value.set(
      key,
      tableMetadata.columns.filter(
        (col) =>
          !col.id.startsWith("mg_") &&
          !unfilterable.includes(col.columnType) &&
          col.showFilter !== false
      )
    );
  } catch (error) {
    console.error("Failed to fetch ref metadata", error);
  }
}
</script>

<template>
  <div
    class="rounded-t-3px rounded-b-50px overflow-hidden pb-8"
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

    <template v-for="column in filterableColumnsComputed" :key="column.id">
      <FilterColumn
        :column="column"
        :schema-id="schemaId"
        :model-value="getFilterValue(column.id)"
        @update:model-value="setFilterValue(column.id, $event)"
        :mobile-display="mobileDisplay"
        :depth="0"
        :removable="true"
        @remove="handleFilterToggle(column.id)"
      />
      <FilterColumn
        v-for="nestedColumn in visibleNestedColumns(column.id)"
        :key="`${column.id}.${nestedColumn.id}`"
        :column="nestedColumn"
        :schema-id="column.refSchemaId || schemaId"
        :label-prefix="`${column.label}.`"
        :model-value="getFilterValue(`${column.id}.${nestedColumn.id}`)"
        @update:model-value="
          setFilterValue(`${column.id}.${nestedColumn.id}`, $event)
        "
        :mobile-display="mobileDisplay"
        :depth="1"
        :removable="true"
        @remove="handleFilterToggle(`${column.id}.${nestedColumn.id}`)"
      />
    </template>

    <template v-for="parent in nestedOnlyParents" :key="`nested-${parent.id}`">
      <FilterColumn
        v-for="nestedColumn in visibleNestedColumns(parent.id)"
        :key="`${parent.id}.${nestedColumn.id}`"
        :column="nestedColumn"
        :schema-id="parent.refSchemaId || schemaId"
        :label-prefix="`${parent.label}.`"
        :model-value="getFilterValue(`${parent.id}.${nestedColumn.id}`)"
        @update:model-value="
          setFilterValue(`${parent.id}.${nestedColumn.id}`, $event)
        "
        :mobile-display="mobileDisplay"
        :depth="0"
        :removable="true"
        @remove="handleFilterToggle(`${parent.id}.${nestedColumn.id}`)"
      />
    </template>
  </div>
</template>
