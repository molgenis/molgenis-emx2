<script setup lang="ts">
import { ref, computed, watch, useId } from "vue";
import type {
  IColumn,
  IRow,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import { useTableData } from "../../composables/useTableData";
import { rowToString } from "../../utils/rowToString";
import InputSearch from "../input/Search.vue";
import InlinePagination from "./InlinePagination.vue";
import LoadingContent from "../LoadingContent.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filter?: object;
    viewColumns?: string[];
    showSearch?: boolean;
    pagingLimit?: number;
    getRefClickAction?: (col: IColumn, row: IRow) => () => void;
  }>(),
  {
    showSearch: true,
    pagingLimit: 10,
  }
);

const searchInputId = useId();
const page = ref(1);
const searchTerms = ref("");

const { metadata, rows, count, status } = useTableData(
  props.schemaId,
  props.tableId,
  {
    pageSize: props.pagingLimit,
    page,
    filter: props.filter,
    searchTerms,
  }
);

const totalPages = computed(() => Math.ceil(count.value / props.pagingLimit));
const showPagination = computed(() => count.value > props.pagingLimit);

const errorText = computed(() => {
  if (status.value === "error") {
    return "Failed to load data";
  }
  return undefined;
});

// construct a ref column for click handling
const refColumn = computed<IRefColumn | undefined>(() => {
  if (!metadata.value) return undefined;
  const keyCol = metadata.value.columns?.find((c) => c.key === 1);
  return {
    id: metadata.value.id,
    label: metadata.value.label,
    columnType: "REF",
    refTableId: metadata.value.id,
    refSchemaId: metadata.value.schemaId,
    refLabel: keyCol?.refLabel || "${name}",
    refLabelDefault: "${name}",
    refLinkId: keyCol?.id || "name",
  };
});

// reset page on search
watch(searchTerms, () => {
  page.value = 1;
});

function handleClick(row: IRow) {
  if (props.getRefClickAction && refColumn.value) {
    props.getRefClickAction(refColumn.value, row)();
  }
}

function getLabel(row: IRow): string {
  const labelTemplate = refColumn.value?.refLabel || "${name}";
  return rowToString(row, labelTemplate) || String(row.name || row.id || "");
}
</script>

<template>
  <div class="emx2-list-view">
    <InputSearch
      v-if="showSearch"
      :id="searchInputId"
      v-model="searchTerms"
      placeholder="Search..."
      size="small"
      class="mb-4"
    />

    <LoadingContent
      :id="`emx2-list-view-${schemaId}-${tableId}`"
      :status="status"
      loading-text="Loading..."
      :error-text="errorText"
      :show-slot-on-error="false"
    >
      <ul v-if="rows.length" class="space-y-1.5 list-none p-0 m-0">
        <li v-for="(row, index) in rows" :key="index">
          <a
            href="#"
            class="text-link hover:text-link-hover hover:underline transition-colors"
            @click.prevent="handleClick(row)"
          >
            {{ getLabel(row) }}
          </a>
        </li>
      </ul>
      <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>

      <InlinePagination
        v-if="showPagination"
        :current-page="page"
        :total-pages="totalPages"
        @update:page="page = $event"
      />
    </LoadingContent>
  </div>
</template>
