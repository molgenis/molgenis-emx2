<template>
  <div>
    <div class="flex mb-[30px] justify-between h-50px">
      <RowControls
        :number-of-selected-rows="numberOfSelectedRows"
        :all-rows-selected="
          numberOfSelectedRows === Math.min(settings.pageSize, rows.length)
        "
        :can-edit="props.isEditable"
        @row-action="handleRowAction"
      />
      <div
        v-if="!props.hideSearch && enableFilters && filters"
        class="shrink-0 w-80 xl:w-96 lg:-ml-[30px] px-5"
      >
        <InputSearch
          class="w-full"
          size="medium"
          v-model="searchValue"
          :placeholder="`Search ${props.tableId}`"
          id="search-input"
        />
      </div>
      <InputSearch
        v-else-if="!props.hideSearch"
        class="w-3/5 xl:w-2/5 2xl:w-1/5"
        size="medium"
        v-model="searchValue"
        :placeholder="`Search ${props.tableId}`"
        id="search-input"
      />

      <div class="flex gap-[10px]">
        <Button
          v-if="props.isEditable && data?.tableMetadata"
          type="primary"
          size="medium"
          icon="add-circle"
          @click="onAddRowClicked"
        >
          Add {{ tableId }}
        </Button>

        <TableControlColumns
          :columns="sortedColumns"
          @update:columns="handleColumnsUpdate"
        />

        <DownloadButton
          v-if="schemaId && tableId"
          :schemaId="schemaId"
          :tableId="tableId"
        />

        <slot name="toolbar-end" />
      </div>
    </div>

    <div
      :class="{
        flex: enableFilters,
        'overflow-hidden': enableFilters,
        'gap-6': enableFilters,
        'lg:-ml-[30px]': enableFilters,
      }"
    >
      <Sidebar
        v-if="enableFilters && filters"
        :collapsed="sidebarCollapsed"
        :active-filter-count="filters.activeFilters.value.length"
        @update:collapsed="sidebarCollapsed = $event"
      >
        <FilterSidebarContent
          :filters="filters"
          :columns="filters.columns.value"
          :schema-id="schemaId"
          :table-id="tableId"
        />
      </Sidebar>

      <div class="flex-1 min-w-0">
        <ActiveFilters
          v-if="enableFilters && filters"
          :filters="filters.activeFilters.value"
          :search-value="filters.searchValue.value"
          @remove="filters.removeFilter"
          @clear-all="filters.clearFilters"
          @clear-search="filters.setSearch('')"
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

          <div
            class="overflow-x-auto overscroll-x-contain bg-table rounded-t-base"
            v-on:scroll.native="handleStickyHeaderOffset"
          >
            <div
              v-if="useStickyHeader"
              class="fixed top-0 z-20 overflow-hidden aria-hidden=true"
              :class="{ hidden: !showStickyHeader }"
            >
              <table
                ref="tableHeaderFixed"
                class="border-0 text-left w-full table-fixed bg-table"
              >
                <TableEMX2Head
                  :schemaId="props.schemaId"
                  :tableId="props.tableId"
                  :settings="settings"
                  :columns="sortedVisibleColumns"
                  :showDraftColumn="showDraftColumn"
                  :isResizing="isResizing"
                  :columnWidths="columnWidths"
                  @sort-requested="handleSortRequest"
                  @start-resize="startResize($event.event, $event.id)"
                />
              </table>
            </div>
            <table ref="table" class="text-left w-full table-fixed">
              <TableEMX2Head
                :schemaId="props.schemaId"
                :tableId="props.tableId"
                :settings="settings"
                :columns="sortedVisibleColumns"
                :showDraftColumn="showDraftColumn"
                :isResizing="isResizing"
                :columnWidths="columnWidths"
                @sort-requested="handleSortRequest"
                @start-resize="startResize($event.event, $event.id)"
              />
              <tbody
                class="mb-3 [&_tr:last-child_td]:border-none [&_tr:last-child_td]:pb-last-row-cell"
              >
                <tr
                  v-if="rows"
                  v-for="row in rows"
                  class="group h-[50px]"
                  :class="{
                    'hover:cursor-pointer': props.isEditable,
                  }"
                >
                  <TableCellEMX2
                    class="sticky left-0 bg-table group-hover:bg-hover z-10 w-12 p-0"
                  >
                    <div class="flex justify-center items-center h-full">
                      <Checkbox
                        :model-value="selectedRows.has(row._rowIdString)"
                        @update:model-value="toggleRowSelection(row)"
                      />
                    </div>
                  </TableCellEMX2>

                  <TableCellEMX2
                    v-if="showDraftColumn"
                    class="text-table-row group-hover:bg-hover"
                  >
                    <DraftLabel v-if="row?.mg_draft === true" type="inline" />
                  </TableCellEMX2>

                  <TableCellEMX2
                    v-for="(column, colIndex) in sortedVisibleColumns"
                    :style="{ width: columnWidths[column.id] + 'px' }"
                    class="text-table-row group-hover:bg-hover"
                    :class="{
                      'w-60 lg:w-full': columns.length <= 5,
                      'w-60': columns.length > 5,
                      'h-11': !row[column.id],
                    }"
                    :scope="column.key === 1 ? 'row' : null"
                    :metadata="column"
                    :data="row[column.id]"
                    @cellClicked="handleCellClick"
                  >
                    <template #row-actions v-if="colIndex === 0">
                      <div
                        class="absolute left-12 h-10 -mt-2 z-10 text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none group-hover:flex flex-row items-center justify-start flex-nowrap gap-1"
                      >
                        <Button
                          v-if="isEditable"
                          :id="`delete-button-${row._rowIdString}`"
                          :icon-only="true"
                          type="inline"
                          icon="trash"
                          label="delete"
                          @click="onShowDeleteModal(row)"
                          :aria-controls="`table-emx2-${schemaId}-${tableId}-modal-delete`"
                          aria-haspopup="dialog"
                          :aria-expanded="showDeleteModal"
                        >
                          {{ row._rowIdString }}
                        </Button>
                        <Button
                          v-if="isEditable"
                          :id="`edit-button-${row._rowIdString}`"
                          :icon-only="true"
                          type="inline"
                          icon="edit"
                          label="edit"
                          @click="onShowEditModal(row)"
                          :aria-controls="`table-emx2-${schemaId}-${tableId}-modal-edit`"
                          aria-haspopup="dialog"
                          :aria-expanded="showEditModal"
                        >
                          {{ row._rowIdString }}
                        </Button>

                        <slot name="additional-row-actions" :row="row" />
                      </div>
                    </template>
                  </TableCellEMX2>
                </tr>
              </tbody>
            </table>
            <div
              class="sticky left-0 flex justify-center items-center py-2.5"
              v-if="status === 'success' && !rows?.length"
            >
              <TextNoResultsMessage
                class="w-full text-center"
                :label="emptyRowsLabel"
              />
            </div>
          </div>
        </div>

        <div
          class="p-2.5 text-right font-normal align-middle text-table-column-header"
        >
          {{ countMessage }}
        </div>

        <Pagination
          v-if="count > smallestPageSize"
          class="pt-0 pb-[30px]"
          :current-page="settings.page"
          :totalPages="Math.ceil(count / settings.pageSize)"
          :jump-to-edge="true"
          :page-size="settings.pageSize"
          :show-page-size-selector="true"
          @update="handlePagingRequest($event)"
          @update:pageSize="handlePageSizeChange($event)"
        />
      </div>
    </div>
  </div>

  <CellDetailModal
    v-if="cellDetailPayload"
    :payload="cellDetailPayload"
    :schemaId="schemaId"
    v-model:showModal="showModal"
    @update:cellDetailPayload="cellDetailPayload = $event"
  />

  <DeleteModal
    v-if="data?.tableMetadata && rowDataForModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :formValues="rowDataForModal"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
  />

  <DeleteRows
    v-if="data?.tableMetadata && showDeleteMultipleModal"
    :schemaId="props.schemaId"
    :metadata="data.tableMetadata"
    :keys="new Set(selectedRows.values())"
    v-model:visible="showDeleteMultipleModal"
    @update:deleted="afterRowDeleted"
  />

  <EditModal
    v-if="data?.tableMetadata && showEditModal"
    :key="`edit-modal-${rowDataForModal?._rowIdString}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :schema="data.schemaMetadata"
    :formValues="rowDataForModal"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterClose"
  />

  <EditModal
    v-if="data?.tableMetadata && showAddModal"
    :key="`add-modal-${tableId}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :schema="data.schemaMetadata"
    :isInsert="true"
    v-model:visible="showAddModal"
    @update:cancelled="afterClose"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../../metadata-utils/src/types";
import type {
  cellPayload,
  ITableSettings,
  sortDirection,
} from "../../../types/types";
import { sortColumns } from "../../utils/sortColumns";

import fetchTableData from "../../composables/fetchTableData";
import fetchMetadata from "../../composables/fetchMetadata";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { getPrimaryKey } from "../../utils/getPrimaryKey";

import type { IGraphQLFilter } from "../../../types/filters";
import type { UseFilters } from "../../../types/filters";
import { useFilters } from "../../composables/useFilters";
import TableCellEMX2 from "./CellEMX2.vue";

import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";
import InputSearch from "../input/Search.vue";
import Sidebar from "../Sidebar.vue";
import FilterSidebarContent from "../filter/SidebarContent.vue";
import ActiveFilters from "../filter/ActiveFilters.vue";

import { useAsyncData } from "nuxt/app";
import { useColumnResize } from "../../composables/useColumnResize";
import constants from "../../utils/constants";
import { getCountMessage } from "../../utils/getCountMessage";
import Button from "../Button.vue";
import Pagination from "../Pagination.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import DraftLabel from "../label/DraftLabel.vue";
import Checkbox from "../input/Checkbox.vue";
import CellDetailModal from "./cellDetail/CellDetailModal.vue";
import RowControls from "./control/RowControls.vue";
import DeleteRows from "./control/DeleteRows.vue";
import TableControlColumns from "./control/Columns.vue";
import TableEMX2Head from "./TableEMX2Head.vue";
import DownloadButton from "./control/DownloadButton.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    isEditable?: boolean;
    filter?: IGraphQLFilter;
    hideSearch?: boolean;
    enableFilters?: boolean;
    useStickyHeader?: boolean;
  }>(),
  {
    isEditable: () => false,
    filter: () => ({}),
    hideSearch: false,
    enableFilters: true,
    useStickyHeader: () => true,
  }
);

const emit = defineEmits<{
  (e: "view-details", payload: TableRow): void;
}>();

const showAddModal = ref<boolean>(false);
const showEditModal = ref<boolean>(false);
const showDeleteModal = ref<boolean>(false);
const showDeleteMultipleModal = ref<boolean>(false);
const rowDataForModal = ref<IRow>();
const showModal = ref(false);

const cellDetailPayload = ref<cellPayload>();
const columns = ref<IColumn[]>([]);
const showStickyHeader = ref(false);
const selectedRows = ref<Map<string, Record<string, columnValue>>>(new Map());

const tableContainer = ref<HTMLElement | null>(null);
const tableHeaderFixed = ref<HTMLElement | null>(null);
const tableHead = ref<HTMLElement | null>(null);
const { columnWidths, guideX, startResize, setInitialWidths, isResizing } =
  useColumnResize(tableContainer);

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: constants.PAGE_SIZE_DEFAULT,
    orderby: { column: "", direction: "ASC" },
    search: "",
    orderedColumnsIds: [],
  }),
});

type TableRow = {
  _rowId: Record<string, columnValue>;
  _rowIdString: string;
} & Record<string, columnValue>;

const filters: UseFilters | null = props.enableFilters
  ? useFilters(
      computed(() => columns.value),
      {
        urlSync: true,
        schemaId: props.schemaId,
        tableId: props.tableId,
      }
    )
  : null;

const sidebarCollapsed = ref(false);

if (filters) {
  watch(
    () => filters.searchValue.value,
    (value) => {
      settings.value.search = value ?? "";
      settings.value.page = 1;
    }
  );
}

const searchValue = computed({
  get: () =>
    props.enableFilters && filters
      ? filters.searchValue.value
      : settings.value.search,
  set: (val: string) => {
    if (props.enableFilters && filters) {
      filters.setSearch(val);
    } else {
      handleSearchRequest(val);
    }
  },
});

const effectiveFilter = computed(() =>
  filters ? filters.gqlFilter.value : props.filter
);

const { data, refresh, status } = useAsyncData(
  `tableEMX2-${props.schemaId}-${props.tableId}`,
  async () => {
    const tableMetadata = await fetchTableMetadata(
      props.schemaId,
      props.tableId
    );

    const schemaMetadata = await fetchMetadata(props.schemaId);

    const tableData = await fetchTableData(props.schemaId, props.tableId, {
      limit: settings.value.pageSize,
      offset: (settings.value.page - 1) * settings.value.pageSize,
      orderby: settings.value.orderby.column
        ? { [settings.value.orderby.column]: settings.value.orderby.direction }
        : {},
      searchTerms: settings.value.search,
      filter: effectiveFilter.value,
    });

    // add unique row identifier for selection purposes
    const rows: TableRow[] = await Promise.all(
      tableData.rows.map(async (row) => {
        const primaryKey = await getPrimaryKey(
          row,
          props.tableId,
          props.schemaId
        );
        return {
          ...row,
          _rowId: primaryKey,
          _rowIdString: JSON.stringify(primaryKey),
        };
      })
    );

    return {
      tableMetadata,
      schemaMetadata,
      rows,
      count: tableData.count,
    };
  }
);

onMounted(async () => {
  sidebarCollapsed.value = window.matchMedia("(max-width: 1023px)").matches;
  if (props.useStickyHeader) {
    window.addEventListener("resize", updateStickyHeaderWidth);
    window.addEventListener("scroll", handleStickyHeaderScroll);
  }
});

onUnmounted(async () => {
  window.removeEventListener("scroll", handleStickyHeaderScroll);
  window.removeEventListener("resize", updateStickyHeaderWidth);
});

const countMessage = computed(() =>
  getCountMessage(settings.value.page, settings.value.pageSize, count.value)
);

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

let widthsInitialized = false;
watch(
  () => columns.value,
  (newColumns) => {
    if (
      !widthsInitialized &&
      Array.isArray(newColumns) &&
      newColumns.length > 0
    ) {
      setInitialWidths(newColumns);
      widthsInitialized = true;
    }
  },
  { immediate: true, deep: true }
);

const rows = computed((): TableRow[] =>
  Array.isArray(data.value?.rows) ? data.value?.rows : []
);

const hasFiltersOrSearch = computed(
  () =>
    (filters?.activeFilters.value.length ?? 0) > 0 ||
    (filters?.searchValue.value ?? "").length > 0
);

const emptyRowsLabel = computed(() =>
  hasFiltersOrSearch.value ? "No data matched the filters" : "No records found"
);

const showDraftColumn = computed(() =>
  rows.value.some((row: TableRow) => row?.mg_draft === true)
);

const count = computed(() => data.value?.count ?? 0);

watch(
  () => effectiveFilter.value,
  () => {
    settings.value.page = 1;
    refresh();
  },
  { deep: true }
);

watch(
  () => data.value?.tableMetadata,
  (newMetadata) => {
    if (newMetadata) {
      columns.value = newMetadata.columns.filter(
        (c: IColumn) =>
          !c.id.startsWith("mg") &&
          !["HEADING", "SECTION"].includes(c.columnType)
      );
    }
  },
  { immediate: true }
);

const sortedColumns = computed(() => {
  // sort from backend
  let sortedColumns = sortColumns([...(columns.value ?? [])]);

  if (settings.value.orderedColumnsIds?.length) {
    // override visibility with user settings
    sortedColumns = sortedColumns.map((col) => {
      return {
        ...col,
        // use string instead of boolean for compatibility backend
        visible: settings.value.orderedColumnsIds.includes(col.id)
          ? "true"
          : "false",
      };
    });
    // order by user settings
    sortedColumns.sort((a, b) => {
      const indexA = settings.value.orderedColumnsIds?.indexOf(a.id) ?? -1;
      const indexB = settings.value.orderedColumnsIds?.indexOf(b.id) ?? -1;
      return indexA - indexB;
    });
  }

  return sortedColumns;
});

const sortedVisibleColumns = computed(() =>
  sortedColumns.value.filter((col) => col.visible !== "false")
);

const numberOfSelectedRows = computed(() => selectedRows.value.size);

function handleColumnsUpdate(newColumns: IColumn[]) {
  settings.value.orderedColumnsIds = newColumns.map((col) => col.id);
}

function toggleRowSelection(row: TableRow) {
  if (selectedRows.value.has(row._rowIdString)) {
    selectedRows.value.delete(row._rowIdString);
  } else {
    selectedRows.value.set(row._rowIdString, row._rowId);
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.size === 1
        ? rows.value.find((row) => selectedRows.value.has(row._rowIdString))
        : null;
    switch (action) {
      case "delete-selection":
        if (singleRowSelected) {
          onShowDeleteModal(singleRowSelected);
        } else if (selectedRows.value.size > 1) {
          showDeleteMultipleModal.value = true;
        }
        break;
      case "edit-selection":
        if (singleRowSelected) {
          onShowEditModal(singleRowSelected);
        }
        break;
      case "view-details":
        if (singleRowSelected) {
          emit("view-details", singleRowSelected);
        }
        break;
      case "select-all-on-page":
        rows.value.forEach((row) => {
          selectedRows.value.set(row._rowIdString, row._rowId);
        });
        break;
      case "select-none":
        selectedRows.value.clear();
        break;
      case "select-drafts":
        selectedRows.value.clear();
        rows.value.forEach((row) => {
          if (row.mg_draft === true) {
            selectedRows.value.set(row._rowIdString, row._rowId);
          }
        });
        break;
    }
  }
}

function handleSortRequest(columnId: string) {
  const direction: sortDirection = getDirection(columnId);
  settings.value.orderby.column = columnId;
  settings.value.orderby.direction = direction;
  settings.value.page = 1;
  refresh();
}

function getDirection(columnId: string): sortDirection {
  if (settings.value.orderby.column === columnId) {
    return settings.value.orderby.direction === "ASC" ? "DESC" : "ASC";
  } else {
    return "ASC";
  }
}

function handleSearchRequest(search: string) {
  settings.value.search = search;
  settings.value.page = 1;
  refresh();
}

const smallestPageSize = computed(() =>
  Math.min(...constants.PAGE_SIZE_OPTIONS)
);

function handlePagingRequest(page: number) {
  settings.value.page = page;
  refresh();
}

function handlePageSizeChange(pageSize: string) {
  settings.value.pageSize = Number.parseInt(pageSize);
  settings.value.page = 1;
  refresh();
}

function handleCellClick(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showModal.value = true;
}

function onShowDeleteModal(row: TableRow) {
  rowDataForModal.value = row;
  showDeleteModal.value = true;
}

function onShowEditModal(row: TableRow) {
  const clone: IRow = structuredClone(row);
  delete clone._rowId;
  delete clone._rowIdString;
  rowDataForModal.value = clone;
  showEditModal.value = true;
}

function onAddRowClicked() {
  showAddModal.value = true;
}

async function afterClose() {
  await refresh();
}

async function afterRowDeleted() {
  // maybe notify user, and do more stuff
  await refresh();
}
</script>
