<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";
import FilterColumn from "./Column.vue";
import InputSearch from "../input/Search.vue";
import Columns from "../table/control/Columns.vue";
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

const filterableColumnsComputed = computed<IColumn[]>(() => {
  const unfilterableTypes = ["HEADING", "SECTION"];

  return props.allColumns.filter(
    (col) =>
      !unfilterableTypes.includes(col.columnType) &&
      !col.id.startsWith("mg_") &&
      col.showFilter !== false
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

function handleColumnsUpdate(updatedColumns: IColumn[]) {
  emit("update:columns", updatedColumns);
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
    class="rounded-t-3px rounded-b-50px"
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
      <Columns
        mode="filters"
        :columns="allColumns"
        label="Customize"
        icon="settings"
        button-type="text"
        size="tiny"
        @update:columns="handleColumnsUpdate"
      />
    </div>

    <div v-if="showSearch" class="px-5 pb-5">
      <InputSearch
        :id="searchInputId"
        v-model="searchTerms"
        placeholder="Search..."
        size="small"
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
        @expand="handleExpand(column)"
      />
      <div
        v-if="expandedRefs.has(column.id)"
        class="ml-4 border-l-2 border-black/10"
      >
        <FilterColumn
          v-for="nestedColumn in refColumnsCache.get(column.id)"
          :key="`${column.id}.${nestedColumn.id}`"
          :column="nestedColumn"
          :model-value="getFilterValue(`${column.id}.${nestedColumn.id}`)"
          @update:model-value="
            setFilterValue(`${column.id}.${nestedColumn.id}`, $event)
          "
          :collapsed="true"
          :mobile-display="mobileDisplay"
          :depth="1"
        />
      </div>
    </template>
  </div>
</template>
