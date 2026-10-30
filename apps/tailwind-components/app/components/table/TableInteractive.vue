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
import type { cellPayload, ITableSettings } from "../../../types/types.ts";
import { useColumnResize } from "../../composables/useColumnResize";
import { FILTER_DEBOUNCE } from "../../composables/useFilters";
import { getCountMessage } from "../../utils/getCountMessage";
import ActiveFilters from "../filter/ActiveFilters.vue";
import FilterSidebarContent from "../filter/SidebarContent.vue";
import Checkbox from "../input/Checkbox.vue";
import InputSearch from "../input/Search.vue";
import DraftLabel from "../label/DraftLabel.vue";
import Pagination from "../Pagination.vue";
import Sidebar from "../Sidebar.vue";
import TableCellEMX2 from "./CellEMX2.vue";
import RowControls from "./control/RowControls.vue";

const STICKY_SELECT_COLUMN_WIDTH = 48;

const props = withDefaults(
  defineProps<{
    features: FeatureFlags;
    filters?: UseFilters | null;
    permissions: Permissions;
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
  (event: "cellClicked", payload: cellPayload): void;
}>();

const selectColumnStyle = {
  width: `${STICKY_SELECT_COLUMN_WIDTH}px`,
};

const sidebarCollapsed = ref(false);
const showStickyHeader = ref(false);
const tableContainer = ref<HTMLElement | null>(null);
const tableHeaderFixed = ref<HTMLElement | null>(null);
const tableHead = ref<HTMLElement | null>(null);
const { columnWidths, guideX, startResize, setInitialWidths, isResizing } =
  useColumnResize(tableContainer);

const columns = computed(
  () => props.sortedVisibleColumns || props.tableMetadata.columns
);
const numberOfSelectedRows = computed(() => props.selectedRows?.size || 0);
const hasFiltersOrSearch = computed(
  () =>
    (props.filters?.activeFilters.value.length ?? 0) ||
    (props.filters?.searchValue.value ?? "").length
);
const emptyRowsLabel = computed(() =>
  hasFiltersOrSearch.value ? "No data matched the filters" : "No records found"
);
const handleSearchChange = useDebounceFn((search?: string) => {
  emit("update:settings", { ...props.settings, search, page: 1 });
}, FILTER_DEBOUNCE);
const countMessage = computed(() =>
  getCountMessage(props.settings.page, props.settings.pageSize, props.rowCount)
);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(props.rowCount / props.settings.pageSize))
);

onMounted(async () => {
  setInitialWidths(columns.value);
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

function handleRowAction(payload: { action: string }) {
  emit("rowAction", payload);
}

function handleStickyHeaderScroll(event: Event) {
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

function handleSortRequest(columnId: string) {
  const isSameColumn = props.settings.orderby.column === columnId;
  const newDirection =
    isSameColumn && props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";

  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction: newDirection },
  });
}

function toggleRowSelection(row: TableRow) {
  emit("toggleRowSelection", row);
}

function handleCellClick(payload: cellPayload) {
  emit("cellClicked", payload);
}

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

interface FeatureFlags {
  showSearch?: boolean;
  showFilters?: boolean;
  showStickyHeader?: boolean;
  showDraftColumn?: boolean;
  showRolesColumn?: boolean;
}

interface Permissions {
  canUpdate: boolean;
  canInsert: boolean;
  canDelete: boolean;
  canModifySelection: boolean;
  isRowLevel: boolean;
}
</script>

<template>
  <div>
    <div class="flex mb-[30px] justify-between h-50px">
      <RowControls
        :numberOfSelectedRows="numberOfSelectedRows"
        :allRowsSelected="
          numberOfSelectedRows === Math.min(settings.pageSize, rows.length)
        "
        :canUpdate="permissions.canUpdate"
        :canDelete="permissions.canDelete"
        :canModifySelection="permissions.canModifySelection"
        @rowAction="handleRowAction"
      />

      <InputSearch
        v-if="features.showSearch"
        class="w-3/5 xl:w-2/5 2xl:w-1/5"
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
        <ActiveFilters
          v-if="features.showFilters && filters"
          :filters="filters.activeFilters.value"
          :searchValue="filters.searchValue.value"
          @remove="filters.removeFilter"
          @clearAll="filters.clearFilters"
          @clearSearch="filters.setSearch('')"
        />
        <slot v-else name="active-filters" />

        <div
          ref="tableContainer"
          class="relative overflow-auto overflow-y-hidden rounded-t-base rounded-b-alt border border-theme border-color-theme"
        >
          <div
            v-if="guideX !== null"
            class="absolute top-0 bottom-0 w-[2px] bg-button-primary pointer-events-none z-50"
            :style="{ left: guideX + 'px' }"
          />

          <ScrollAreaHorizontal
            class="bg-table rounded-t-base"
            :startInset="STICKY_SELECT_COLUMN_WIDTH"
            @scroll="handleStickyHeaderOffset"
          >
            <div
              v-if="features.showStickyHeader"
              class="fixed top-0 z-[36] overflow-hidden aria-hidden=true"
              :class="{ hidden: !showStickyHeader }"
            >
              <table
                ref="tableHeaderFixed"
                class="border-0 text-left w-full table-fixed bg-table"
              >
                <TableEMX2Head
                  :schemaId="tableMetadata.schemaId"
                  :tableId="tableMetadata.id"
                  :settings="settings"
                  :columns="columns"
                  :showDraftColumn="features.showDraftColumn"
                  :showRolesColumn="features.showRolesColumn"
                  :isResizing="isResizing"
                  :columnWidths="columnWidths"
                  :selectColumnWidth="STICKY_SELECT_COLUMN_WIDTH"
                  @sortRequested="handleSortRequest"
                  @startResize="startResize($event.event, $event.id)"
                />
              </table>
            </div>
            <table ref="table" class="text-left w-full table-fixed">
              <TableEMX2Head
                :schemaId="tableMetadata.schemaId"
                :tableId="tableMetadata.id"
                :settings="settings"
                :columns="columns"
                :showDraftColumn="features.showDraftColumn"
                :showRolesColumn="features.showRolesColumn"
                :isResizing="isResizing"
                :columnWidths="columnWidths"
                :selectColumnWidth="STICKY_SELECT_COLUMN_WIDTH"
                @sortRequested="handleSortRequest"
                @startResize="startResize($event.event, $event.id)"
              />
              <tbody class="mb-3 [&_tr:last-child_td]:after:hidden">
                <tr
                  v-if="rows"
                  v-for="row in rows"
                  class="group h-13"
                  :class="{
                    'hover:cursor-pointer':
                      permissions.canInsert ||
                      permissions.canUpdate ||
                      permissions.canDelete,
                  }"
                >
                  <td
                    class="sticky left-0 z-20 group-hover:z-40 w-12 h-13 py-0 px-2.5 align-middle after:absolute after:inset-x-0 after:bottom-0 after:border-b after:pointer-events-none shadow-[inset_-1px_0_0_var(--border-color-theme)] bg-table group-hover:bg-hover"
                    :style="selectColumnStyle"
                  >
                    <Checkbox
                      class="block mx-auto"
                      :modelValue="selectedRows?.has(row._rowIdString)"
                      @update:modelValue="toggleRowSelection(row)"
                    />
                    <div
                      class="absolute left-full top-0 bottom-px flex flex-row text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none items-center justify-start flex-nowrap gap-1"
                    >
                      <slot name="row-action" :row="row" />
                    </div>
                  </td>

                  <TableCellEMX2
                    v-if="features.showRolesColumn"
                    class="text-table-row group-hover:bg-hover w-48 h-13 py-0 pl-2 pr-2.5"
                  >
                    {{ row.mg_roles?.[0] ?? "" }}
                  </TableCellEMX2>

                  <TableCellEMX2
                    v-if="features.showDraftColumn"
                    class="text-table-row group-hover:bg-hover h-13 py-0 pl-2 pr-2.5"
                  >
                    <DraftLabel v-if="row?.mg_draft === true" type="inline" />
                  </TableCellEMX2>

                  <TableCellEMX2
                    v-for="column in columns"
                    :style="{ width: columnWidths[column.id] + 'px' }"
                    class="text-table-row group-hover:bg-hover h-13 py-0 pl-2 pr-2.5"
                    :class="{
                      'w-60 lg:w-full': columns.length <= 5,
                      'w-60': columns.length > 5,
                    }"
                    :scope="column.key === 1 ? 'row' : null"
                    :metadata="column"
                    :data="row[column.id]"
                    @cellClicked="handleCellClick"
                  />

                  <!-- Matches the filler column in the header -->
                  <td
                    aria-hidden="true"
                    class="relative after:absolute after:inset-x-0 after:bottom-0 after:border-b after:pointer-events-none after:z-[35] group-hover:bg-hover"
                  />
                </tr>
              </tbody>
            </table>
            <div
              class="sticky left-0 flex justify-center items-center py-2.5"
              v-if="!rows?.length"
            >
              <TextNoResultsMessage
                class="w-full text-center"
                :label="emptyRowsLabel"
              />
            </div>
          </ScrollAreaHorizontal>
        </div>

        <div
          class="p-2.5 text-right font-normal align-middle text-table-column-header"
        >
          {{ countMessage }}
        </div>

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
  </div>
</template>
