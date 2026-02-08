<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import FilterPicker from "./FilterPicker.vue";

import fetchTableMetadata from "../../composables/fetchTableMetadata";

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
const REF_TYPES_FOR_DEFAULT = ["REF", "REF_ARRAY"];
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
    return urlParam.split(",").map((id) => id.trim()).filter(Boolean);
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
  } else {
    visibleFilterIds.value = [...visibleFilterIds.value, columnId];
  }
}

function handleFilterReset() {
  visibleFilterIds.value = [...defaultFilterIds.value];
}

const directFilterIds = computed(() => {
  return new Set(
    visibleFilterIds.value.filter((id) => !id.includes("."))
  );
});

const nestedParentIds = computed(() => {
  const parents = new Set<string>();
  for (const id of visibleFilterIds.value) {
    if (id.includes(".")) {
      parents.add(id.split(".")[0]);
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

const expandedRefs = ref<Set<string>>(new Set());
const refColumnsCache = ref<Map<string, IColumn[]>>(new Map());

const nestedFilterIds = computed(() => {
  const nested = new Map<string, string[]>();
  for (const id of visibleFilterIds.value) {
    if (id.includes(".")) {
      const [parentId, childId] = id.split(".", 2);
      if (!nested.has(parentId)) nested.set(parentId, []);
      nested.get(parentId)!.push(childId);
    }
  }
  return nested;
});

watch(nestedFilterIds, async (newNested) => {
  for (const parentId of newNested.keys()) {
    if (!expandedRefs.value.has(parentId)) {
      const parentCol = props.allColumns.find((c) => c.id === parentId);
      if (parentCol) await handleExpand(parentCol);
    }
  }
}, { immediate: true });

function visibleNestedColumns(parentId: string): IColumn[] {
  const all = refColumnsCache.value.get(parentId) || [];
  const selectedChildIds = nestedFilterIds.value.get(parentId);
  if (!selectedChildIds) return all;
  return all.filter((c) => selectedChildIds.includes(c.id));
}

function getFilterValue(columnId: string): IFilterValue | null {
  return filterStates.value.get(columnId) || null;
}

function setFilterValue(
  columnId: string,
  value: IFilterValue | null | undefined
) {
  const newMap = new Map(filterStates.value);
  if (value === null || value === undefined) {
    newMap.delete(columnId);
  } else {
    newMap.set(columnId, value);
  }
  filterStates.value = newMap;
}

async function handleExpand(column: IColumn) {
  const key = column.id;

  if (expandedRefs.value.has(key)) {
    const newSet = new Set(expandedRefs.value);
    newSet.delete(key);
    expandedRefs.value = newSet;
    return;
  }

  if (!refColumnsCache.value.has(key)) {
    const refSchemaId = column.refSchemaId || props.schemaId;
    const refTableId = column.refTableId;

    if (!refTableId) return;

    try {
      const tableMetadata = await fetchTableMetadata(refSchemaId, refTableId);
      const unfilterableTypes = ["HEADING", "SECTION", "REFBACK"];
      const filteredColumns = tableMetadata.columns.filter(
        (col) =>
          !col.id.startsWith("mg_") &&
          !unfilterableTypes.includes(col.columnType) &&
          col.showFilter !== false
      );
      refColumnsCache.value.set(key, filteredColumns);
    } catch (error) {
      console.error("Failed to fetch ref metadata", error);
      return;
    }
  }

  const newSet = new Set(expandedRefs.value);
  newSet.add(key);
  expandedRefs.value = newSet;
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
        :model-value="getFilterValue(column.id)"
        @update:model-value="setFilterValue(column.id, $event)"
        :collapsed="true"
        :mobile-display="mobileDisplay"
        :depth="0"
        :removable="true"
        @remove="handleFilterToggle(column.id)"
      />
      <div
        v-if="expandedRefs.has(column.id)"
        class="ml-4 border-l-2 border-black/10"
      >
        <FilterColumn
          v-for="nestedColumn in refColumnsCache.get(column.id)"
          :key="`${column.id}.${nestedColumn.id}`"
          :column="nestedColumn"
          :label-prefix="`${column.label}.`"
          :model-value="getFilterValue(`${column.id}.${nestedColumn.id}`)"
          @update:model-value="
            setFilterValue(`${column.id}.${nestedColumn.id}`, $event)
          "
          :collapsed="true"
          :mobile-display="mobileDisplay"
          :depth="1"
          :removable="true"
          @remove="handleFilterToggle(`${column.id}.${nestedColumn.id}`)"
        />
      </div>
    </template>

    <template v-for="parent in nestedOnlyParents" :key="`nested-${parent.id}`">
      <FilterColumn
        v-for="nestedColumn in visibleNestedColumns(parent.id)"
        :key="`${parent.id}.${nestedColumn.id}`"
        :column="nestedColumn"
        :label-prefix="`${parent.label}.`"
        :model-value="getFilterValue(`${parent.id}.${nestedColumn.id}`)"
        @update:model-value="
          setFilterValue(`${parent.id}.${nestedColumn.id}`, $event)
        "
        :collapsed="true"
        :mobile-display="mobileDisplay"
        :depth="0"
        :removable="true"
        @remove="handleFilterToggle(`${parent.id}.${nestedColumn.id}`)"
      />
    </template>
  </div>
</template>
