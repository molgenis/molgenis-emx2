<script setup lang="ts">
import { useDebounceFn } from "@vueuse/core";
import { computed, onMounted, onUnmounted, ref } from "vue";
import type {
  columnValue,
  IColumn,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types.ts";
import type { UseFilters } from "../../../types/filters.ts";
import type { ITableSettings } from "../../../types/types.ts";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import FilterSidebarContent from "../filter/SidebarContent.vue";
import Checkbox from "../input/Checkbox.vue";
import InputSearch from "../input/Search.vue";
import Pagination from "../Pagination.vue";
import Sidebar from "../Sidebar.vue";
import Table from "../Table.vue";
import TableCell from "../TableCell.vue";
import TableCellEMX2 from "./CellEMX2.vue";
import RowControls from "./control/RowControls.vue";
import TableHeadCell from "./TableHeadCell.vue";
import TableHeaderAction from "./TableHeaderAction.vue";

const props = withDefaults(
  defineProps<{
    features: {
      showSearch: boolean;
      showFilters: boolean;
      showStickyHeader: boolean;
    };
    filters?: UseFilters | null;
    rights: {
      canUpdate: boolean;
      canDelete: boolean;
      canModifySelection: boolean;
      isRowLevel: boolean;
    };
    rows: TableRow[];
    rowCount: number;
    settings: ITableSettings;
    searchPlaceholder?: string;
    selectedRows?: Map<string, Record<string, columnValue>>;
    sortedVisibleColumns?: IColumn[];
    tableMetadata: ITableMetaData;
  }>(),
  {
    searchPlaceholder: "Search",
  }
);

const emit = defineEmits<{
  (event: "update:settings", value: ITableSettings): void;
  (event: "rowAction", payload: { action: string }): void;
  (event: "toggleRowSelection", row: TableRow): void;
}>();

const sidebarCollapsed = ref(false);
const showStickyHeader = ref(false);
const tableContainer = ref<HTMLElement | null>(null);
const tableHeaderFixed = ref<HTMLElement | null>(null);
const tableHead = ref<HTMLElement | null>(null);

const numberOfSelectedRows = computed(() => props.selectedRows?.size || 0);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.rowCount / props.settings.pageSize))
);
const columns = computed(() => {
  return props.sortedVisibleColumns || props.tableMetadata.columns;
});

onMounted(async () => {
  sidebarCollapsed.value = window.matchMedia("(max-width: 1023px)").matches;
  if (props.features.showStickyHeader) {
    window.addEventListener("resize", updateStickyHeaderWidth);
    window.addEventListener("scroll", handleStickyHeaderScroll);
  }
});

onUnmounted(async () => {
  window.removeEventListener("scroll", handleStickyHeaderScroll);
  window.removeEventListener("resize", updateStickyHeaderWidth);
});

function handlePagingRequest(page: number) {
  emit("update:settings", { ...props.settings, page });
}

function handlePageSizeChange(pageSize: string) {
  emit("update:settings", {
    ...props.settings,
    pageSize: Number.parseInt(pageSize),
    page: 1,
  });
}

const handleSearchChange = useDebounceFn((search?: string) => {
  emit("update:settings", { ...props.settings, search, page: 1 });
}, FILTER_DEBOUNCE);

function handleSortRequest(columnId: string) {
  const isSameColumn = props.settings.orderby.column === columnId;
  const newDirection =
    isSameColumn && props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";

  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction: newDirection },
  });
}

function handleRowAction(payload: { action: string }) {
  emit("rowAction", payload);
}

function toggleRowSelection(row: TableRow) {
  emit("toggleRowSelection", row);
}

function handleStickyHeaderScroll() {
  const rect = tableContainer?.value?.getBoundingClientRect();
  const top = rect?.top ?? 0;
  showStickyHeader.value = top <= 0;
  updateStickyHeaderWidth();
  const tableHeadHeight = tableHead.value?.getBoundingClientRect().height ?? 0;
  if (rect?.bottom && rect?.bottom <= tableHeadHeight) {
    showStickyHeader.value = false;
  }
}

function handleStickyHeaderOffset(event: Event) {
  const target = event.target as HTMLElement;
  const { scrollLeft } = target;
  if (tableHeaderFixed.value) {
    tableHeaderFixed.value.style.transform = `translateX(-${scrollLeft}px)`;
  }
  updateStickyHeaderWidth();
}

function updateStickyHeaderWidth() {
  const tableFixedContainer = tableHeaderFixed.value?.parentElement;
  if (tableFixedContainer) {
    tableFixedContainer.style.width =
      tableFixedContainer.parentElement?.clientWidth + "px";
  }
}
</script>

<template>
  <div class="flex mb-[30px] justify-between h-50px w-full">
    <RowControls
      :numberOfSelectedRows="numberOfSelectedRows"
      :allRowsSelected="
        numberOfSelectedRows === Math.min(settings.pageSize, rows.length)
      "
      :canUpdate="props.rights.canUpdate"
      :canDelete="props.rights.canDelete"
      :canModifySelection="props.rights.canModifySelection"
      @rowAction="handleRowAction"
    />

    <InputSearch
      v-if="features.showSearch"
      class="shrink-0 w-80 xl:w-96 lg:-ml-[30px] px-5"
      id="search-input"
      size="medium"
      :modelValue="settings.search"
      :placeholder="searchPlaceholder"
      @update:modelValue="handleSearchChange($event)"
    />

    <slot name="buttons" />
  </div>

  <div
    :class="{
      flex: features.showFilters,
      'overflow-hidden': features.showFilters,
      'gap-6': features.showFilters,
      'lg:-ml-[30px]': features.showFilters,
    }"
  >
    <Sidebar
      v-if="features.showFilters && filters"
      :collapsed="sidebarCollapsed"
      :activeFilterCount="filters.activeFilters.value.length"
      @update:collapsed="sidebarCollapsed = $event"
    >
      <FilterSidebarContent
        :filters="filters"
        :columns="filters.columns.value"
        :schemaId="tableMetadata.schemaId"
        :tableId="tableMetadata.id"
      />
    </Sidebar>

    <div class="flex-1 min-w-0">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHeadCell class="left-0 bg-table z-20 w-12"> </TableHeadCell>
            <TableHeadCell v-for="column in columns" :key="column.id">
              <TableHeaderAction
                :column="column"
                :settings="settings"
                @sortRequested="handleSortRequest"
              />
            </TableHeadCell>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="(row, index) in rows" :key="index">
            <TableCell
              class="left-0 bg-table group-hover:bg-hover z-10 w-12 h-13 py-0 px-2.5"
            >
              <div class="flex justify-center items-center h-full">
                <Checkbox
                  :modelValue="selectedRows?.has(row._rowIdString)"
                  @update:modelValue="toggleRowSelection(row)"
                />
              </div>
            </TableCell>
            <TableCellEMX2
              v-for="column in columns"
              :key="column.id"
              :metadata="column"
              :data="row[column.id]"
            />
          </TableRow>
        </template>
        <template #foot> </template>
      </Table>

      <Pagination
        class="pt-0 pb-[30px]"
        :currentPage="settings.page"
        :totalPages="totalPages"
        :jumpToEdge="true"
        :pageSize="settings.pageSize"
        :showPageSizeSelector="true"
        @update="handlePagingRequest($event)"
        @update:pageSize="handlePageSizeChange($event)"
      />
    </div>
  </div>
</template>
