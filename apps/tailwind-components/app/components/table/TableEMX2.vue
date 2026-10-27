<template>
  <TableInteractive
    v-if="tableMetadata && rows"
    :features="{
      showSearch: !hideSearch,
      showFilters: enableFilters && !!filters,
      showStickyHeader: !!useStickyHeader,
      showDraftColumn: showDraftColumn,
      showRolesColumn: showRolesColumn,
    }"
    :filters="filters"
    :rights="{
      canUpdate: canUpdate,
      canDelete: canDelete,
      canInsert: canInsert,
      canModifySelection: selectionIsModifiable,
      isRowLevel: isRowLevel,
    }"
    :rows="rows"
    :rowCount="data?.count || 0"
    :settings="settings"
    :selectedRows="selectedRows"
    :sortedVisibleColumns="sortedVisibleColumns"
    :tableMetadata="tableMetadata"
    @update:settings="handleUpdateSettings"
    @rowAction="handleRowAction"
    @toggleRowSelection="handleToggleRowSelection"
    @cellClicked="handleCellClicked"
  >
    <template #buttons>
      <div class="flex gap-[10px]">
        <Button
          v-if="canInsert && tableMetadata"
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

        <Truncate
          v-if="canDelete"
          v-slot="{ showConfirmationModal }"
          :metadata="tableMetadata"
          @update:truncated="afterRowDeleted"
        >
          <Button
            type="outline"
            size="medium"
            @click="showConfirmationModal"
            :disabled="!rows.length"
          >
            Truncate
          </Button>
        </Truncate>
      </div>
    </template>

    <template #row-action="{ row }">
      <div
        class="absolute left-full top-0 bottom-px flex flex-row text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none items-center justify-start flex-nowrap gap-1"
      >
        <Button
          v-if="canDeleteRow(row)"
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
          v-if="canEditRow(row)"
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
        <Button
          v-if="canCloneRow(row)"
          :id="`copy-button-${row._rowIdString}`"
          :icon-only="true"
          type="inline"
          icon="copy"
          label="copy"
          @click="onShowEditModal(row, true)"
          :aria-controls="`table-emx2-${schemaId}-${tableId}-modal-copy`"
          aria-haspopup="dialog"
          :aria-expanded="showEditModal"
        >
          {{ row._rowIdString }}
        </Button>
        <slot name="additional-row-actions" :row="row" />
      </div>
    </template>
  </TableInteractive>

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
    :formValues="rowDataForModal"
    :isInsert="isCopy"
    v-model:visible="showEditModal"
    @update:cancelled="afterClose"
  >
    <template #header="{ formValues }">
      <EditModalHeader
        v-if="formValues"
        :formValues="formValues"
        :isInsert="isCopy"
        :tableId="tableId"
        :schemaId="schemaId"
      />
    </template>
  </EditModal>

  <EditModal
    v-if="data?.tableMetadata && showAddModal"
    :key="`add-modal-${tableId}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="data.tableMetadata"
    :isInsert="true"
    v-model:visible="showAddModal"
    @update:cancelled="afterClose"
  >
    <template #header="{ formValues }">
      <EditModalHeader
        v-if="formValues"
        :formValues="formValues"
        :isInsert="isCopy"
        :tableId="tableId"
        :schemaId="schemaId"
      />
    </template>
  </EditModal>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import type {
  columnValue,
  IColumn,
  IRow,
  ITableMetaData,
  TableRow,
} from "../../../../metadata-utils/src/types";
import type {
  cellPayload,
  ITableSettings,
  sortDirection,
} from "../../../types/types";
import { sortColumns } from "../../utils/sortColumns";

import fetchTableData, {
  type ITableDataResponse,
} from "../../composables/fetchTableData";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { getPrimaryKey } from "../../utils/getPrimaryKey";
import { rowMatchesUserRole } from "../../utils/rowMatchesUserRole";

import type { IGraphQLFilter, UseFilters } from "../../../types/filters";
import { useFilters } from "../../composables/useFilters";

import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";

import { useAsyncData } from "nuxt/app";
import { useColumnResize } from "../../composables/useColumnResize";
import { useSession } from "../../composables/useSession";
import constants from "../../utils/constants";
import { getCountMessage } from "../../utils/getCountMessage";
import Button from "../Button.vue";
import EditModalHeader from "../form/EditModalHeader.vue";
import CellDetailModal from "./cellDetail/CellDetailModal.vue";
import TableControlColumns from "./control/Columns.vue";
import DeleteRows from "./control/DeleteRows.vue";
import DownloadButton from "./control/DownloadButton.vue";
import Truncate from "./control/Truncate.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    canInsert?: boolean;
    canUpdate?: boolean;
    canDelete?: boolean;
    isRowLevel?: boolean;
    userRoles?: string[];
    filter?: IGraphQLFilter;
    hideSearch?: boolean;
    enableFilters?: boolean;
    useStickyHeader?: boolean;
  }>(),
  {
    canInsert: () => false,
    canUpdate: () => false,
    canDelete: () => false,
    isRowLevel: () => false,
    userRoles: () => [],
    filter: () => ({}),
    hideSearch: false,
    enableFilters: true,
    useStickyHeader: () => true,
  }
);

const STICKY_SELECT_COLUMN_WIDTH = 48;

const selectColumnStyle = {
  width: `${STICKY_SELECT_COLUMN_WIDTH}px`,
};

const canEdit = computed(
  () => props.canInsert || props.canUpdate || props.canDelete
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
const isCopy = ref(false);
const sidebarCollapsed = ref(false);

const cellDetailPayload = ref<cellPayload>();
const showStickyHeader = ref(false);
const selectedRows = ref<Map<string, Record<string, columnValue>>>(new Map());

const tableContainer = ref<HTMLElement | null>(null);
const tableHeaderFixed = ref<HTMLElement | null>(null);
const tableHead = ref<HTMLElement | null>(null);
const { columnWidths, guideX, startResize, setInitialWidths, isResizing } =
  useColumnResize(tableContainer);

const tableMetadata = computed<ITableMetaData | null>(
  () => data.value?.tableMetadata ?? null
);

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

const { showRolesForTable } = await useSession(props.schemaId);

const { data, refresh } = useAsyncData(
  `tableEMX2-${props.schemaId}-${props.tableId}`,
  async () => {
    const tableMetadata = await fetchTableMetadata(
      props.schemaId,
      props.tableId
    );

    const tableData: ITableDataResponse = await fetchTableData(
      props.schemaId,
      props.tableId,
      {
        limit: settings.value.pageSize,
        offset: (settings.value.page - 1) * settings.value.pageSize,
        orderby: settings.value.orderby.column
          ? {
              [settings.value.orderby.column]: settings.value.orderby.direction,
            }
          : {},
        searchTerms: settings.value.search,
        filter: effectiveFilter.value,
      }
    );

    const rows: TableRow[] = await addIdentifiersToRows(tableData.rows || []);

    return {
      tableMetadata,
      rows,
      count: tableData.count,
    };
  }
);

const columns = computed<IColumn[]>(
  () =>
    tableMetadata.value?.columns.filter(
      (column: IColumn) =>
        !column.id.startsWith("mg") &&
        !["HEADING", "SECTION"].includes(column.columnType)
    ) ?? []
);

const filters: UseFilters | null = props.enableFilters
  ? useFilters(columns, {
      urlSync: true,
      schemaId: props.schemaId,
      tableId: props.tableId,
    })
  : null;

if (props.enableFilters && filters) {
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

async function addIdentifiersToRows(rows: IRow[]): Promise<TableRow[]> {
  return await Promise.all(
    rows.map(async (row) => {
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

function mayModifyRow(row: IRow): boolean {
  return !props.isRowLevel || rowMatchesUserRole(row, props.userRoles);
}

function canEditRow(row: IRow): boolean {
  return props.canUpdate && mayModifyRow(row);
}

function canCloneRow(row: IRow): boolean {
  return props.canInsert && mayModifyRow(row);
}

function canDeleteRow(row: IRow): boolean {
  return props.canDelete && mayModifyRow(row);
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

const rows = computed((): TableRow[] => data.value?.rows ?? []);

const hasFiltersOrSearch = computed(
  () =>
    (filters?.activeFilters.value.length ?? 0) ||
    (filters?.searchValue.value ?? "").length
);

const emptyRowsLabel = computed(() =>
  hasFiltersOrSearch.value ? "No data matched the filters" : "No records found"
);

const showDraftColumn = computed(() =>
  rows.value.some((row: TableRow) => row?.mg_draft === true)
);

const showRolesColumn = computed(() => {
  return (
    showRolesForTable(props.tableId) &&
    rows.value.some((row: TableRow) => row.mg_roles?.length)
  );
});

const count = computed(() => data.value?.count ?? 0);

watch(
  () => effectiveFilter.value,
  () => {
    settings.value.page = 1;
    refresh();
  },
  { deep: true }
);

const sortedColumns = computed<IColumn[]>(() => {
  const backEndSortedColumns = sortColumns(columns.value);

  if (settings.value.orderedColumnsIds?.length) {
    // order by user settings
    return backEndSortedColumns.sort((a, b) => {
      const indexA = settings.value.orderedColumnsIds?.indexOf(a.id) ?? -1;
      const indexB = settings.value.orderedColumnsIds?.indexOf(b.id) ?? -1;
      return indexA - indexB;
    });
  }

  return backEndSortedColumns;
});

const sortedVisibleColumns = computed<IColumn[]>(() => {
  return sortedColumns.value.filter((col) => col.visible !== "false");
});

const numberOfSelectedRows = computed(() => selectedRows.value.size);

const selectionIsModifiable = computed(
  () =>
    !props.isRowLevel ||
    rows.value
      .filter((row) => selectedRows.value.has(row._rowIdString))
      .every((row) => rowMatchesUserRole(row, props.userRoles))
);

function handleColumnsUpdate(newColumns: IColumn[]) {
  settings.value.orderedColumnsIds = newColumns.map((col) => col.id);
}

function handleToggleRowSelection(row: TableRow) {
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

function handleCellClicked(payload: cellPayload) {
  cellDetailPayload.value = payload;
  showModal.value = true;
}

function onShowDeleteModal(row: TableRow) {
  rowDataForModal.value = row;
  showDeleteModal.value = true;
}

function onShowEditModal(row: TableRow, isCopyMode = false) {
  const clone: IRow = structuredClone(row);
  delete clone._rowId;
  delete clone._rowIdString;
  if (isCopyMode) {
    data.value?.tableMetadata?.columns.forEach((column: IColumn) => {
      if (column.key === 1) {
        delete clone[column.id];
      }
    });
  }
  rowDataForModal.value = clone;
  isCopy.value = isCopyMode;
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

function handleUpdateSettings(newSettings: ITableSettings) {
  settings.value = newSettings;
  searchValue.value = newSettings.search || "";
  refresh();
}
</script>
