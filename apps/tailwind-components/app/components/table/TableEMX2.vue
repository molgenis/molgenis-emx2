<template>
  <div class="flex pb-[30px] justify-between px-2 md:px-0">
    <InputSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      size="medium"
      v-model="settings.search"
      @update:modelValue="handleSearchRequest"
      :placeholder="`Search ${props.tableId}`"
      id="search-input"
    />

    <ActionBar>
      <Button
        v-if="props.isEditable && data?.tableMetadata"
        type="primary"
        size="medium"
        icon="add-circle"
        @click="onAddRowClicked"
        class="whitespace-nowrap"
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

      <Button type="outline" class="whitespace-nowrap" icon="trash"
        >Action 1</Button
      >
      <Button type="outline" class="whitespace-nowrap" icon="edit"
        >Action 2</Button
      >
      <Button type="outline" class="whitespace-nowrap" icon="more-vert"
        >Action 3</Button
      >
      <Button type="outline" class="whitespace-nowrap" icon="download"
        >Action 4</Button
      >
    </ActionBar>
      <Button
        v-if="data?.tableMetadata"
        type="outline"
        :href="`/${schemaId}/api/csv/${tableId}`"
        icon="Download"
        download
      >
        Download
      </Button>
    </div>
  </div>

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
          class="relative overflow-auto overflow-y-hidden rounded-b-theme border border-theme border-color-theme"
        >
          <div
            v-if="guideX !== null"
            class="absolute top-0 bottom-0 w-[2px] bg-button-primary pointer-events-none z-50"
            :style="{ left: guideX + 'px' }"
          />

          <div
            class="overflow-x-auto overscroll-x-contain bg-table rounded-t-3px"
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
                        class="absolute left-2 h-10 -mt-2 z-10 text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none group-hover:flex flex-row items-center justify-start flex-nowrap gap-1"
                      >
                        <Button
                          v-if="isEditable"
                          :id="useId()"
                          :icon-only="true"
                          type="inline"
                          icon="trash"
                          size="small"
                          label="delete"
                          @click="onShowDeleteModal(row)"
                          :aria-controls="`table-emx2-${schemaId}-${tableId}-modal-delete`"
                          aria-haspopup="dialog"
                          :aria-expanded="showDeleteModal"
                        >
                          {{ getRowId(row) }}
                        </Button>
                        <Button
                          v-if="isEditable"
                          :id="useId()"
                          :icon-only="true"
                          type="inline"
                          icon="edit"
                          size="small"
                          label="edit"
                          @click="onShowEditModal(row)"
                          :aria-controls="`table-emx2-${schemaId}-${tableId}-modal-edit`"
                          aria-haspopup="dialog"
                          :aria-expanded="showEditModal"
                        >
                          {{ getRowId(row) }}
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

  <EditModal
    v-if="data?.tableMetadata && showEditModal"
    :key="`edit-modal-${useId()}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :formValues="rowDataForModal"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterClose"
  />

  <EditModal
    v-if="data?.tableMetadata && showAddModal"
    :key="`add-modal-${useId()}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :isInsert="true"
    v-model:visible="showAddModal"
    @update:cancelled="afterClose"
  />
</template>

<script lang="ts">
export function resolveEmptyRowsLabel(hasFiltersOrSearch: boolean): string {
  return hasFiltersOrSearch
    ? "No data matched the filters"
    : "No records found";
}

export function routeSearchValue(
  val: string,
  enableFilters: boolean,
  setSearch: ((v: string) => void) | null,
  handleSearch: (v: string) => void
): void {
  if (enableFilters && setSearch) {
    setSearch(val);
  } else {
    handleSearch(val);
  }
}
</script>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, useId, watch } from "vue";
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

import { fetchTableData, fetchTableMetadata } from "#imports";

import type { IGraphQLFilter } from "../../../types/filters";
import type { UseFilters } from "../../../types/filters";
import { useFilters } from "../../composables/useFilters";
import TableCellEMX2 from "./CellEMX2.vue";
import TableHeadCell from "./TableHeadCell.vue";
import ActionBar from "./ActionBar.vue";

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
import DraftLabel from "../label/DraftLabel.vue";
import Pagination from "../Pagination.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import CellDetailModal from "./cellDetail/CellDetailModal.vue";
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

const showAddModal = ref<boolean>(false);
const showEditModal = ref<boolean>(false);
const showDeleteModal = ref<boolean>(false);
const rowDataForModal = ref();
const showModal = ref(false);

const cellDetailPayload = ref<cellPayload>();
const columns = ref<IColumn[]>([]);
const showStickyHeader = ref(false);
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

onMounted(() => {
  sidebarCollapsed.value = window.matchMedia("(max-width: 1023px)").matches;
});

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
    routeSearchValue(
      val,
      props.enableFilters,
      filters ? filters.setSearch : null,
      handleSearchRequest
    );
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

    const tableData = await fetchTableData(props.schemaId, props.tableId, {
      limit: settings.value.pageSize,
      offset: (settings.value.page - 1) * settings.value.pageSize,
      orderby: settings.value.orderby.column
        ? { [settings.value.orderby.column]: settings.value.orderby.direction }
        : {},
      searchTerms: settings.value.search,
      filter: effectiveFilter.value,
    });

    return {
      tableMetadata,
      tableData,
    };
  }
);

onMounted(async () => {
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

const rows = computed((): IRow[] =>
  Array.isArray(data.value?.tableData?.rows) ? data.value?.tableData?.rows : []
);

const hasFiltersOrSearch = computed(
  () =>
    (filters?.activeFilters.value.length ?? 0) > 0 ||
    (filters?.searchValue.value ?? "").length > 0
);

const emptyRowsLabel = computed(() =>
  resolveEmptyRowsLabel(hasFiltersOrSearch.value)
);

const showDraftColumn = computed(() =>
  rows.value.some((row: IRow) => row?.mg_draft === true)
);

const count = computed(() => data.value?.tableData?.count ?? 0);

const primaryKeys = computed(() => {
  return columns.value
    ?.map((col: IColumn) => {
      if (Object.hasOwn(col, "key")) {
        return col.id;
      }
    })
    .filter((value: any) => value);
});

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

function handleColumnsUpdate(newColumns: IColumn[]) {
  settings.value.orderedColumnsIds = newColumns.map((col) => col.id);
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

function getRowId(row: IRow) {
  return primaryKeys.value
    .map((key) => row[key as string])
    .join("-")
    .replaceAll(" ", "-");
}

function onShowDeleteModal(row: Record<string, columnValue>) {
  rowDataForModal.value = row;
  showDeleteModal.value = true;
}

function onShowEditModal(row: Record<string, columnValue>) {
  rowDataForModal.value = row;
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
