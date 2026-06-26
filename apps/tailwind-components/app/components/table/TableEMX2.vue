<template>
  <div class="flex pb-4 justify-between h-50px">
    <RowControles
      :number-of-selected-rows="numberOfSelectedRows"
      :all-rows-selected="allRowsSelected"
      :can-edit="props.isEditable"
      @row-action="handleRowAction"
    />
    <InputSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      size="medium"
      v-model="settings.search"
      @update:modelValue="handleSearchRequest"
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
        class="h-50px"
      >
        Add {{ tableId }}
      </Button>

      <TableControlColumns
        :columns="sortedColumns"
        @update:columns="handleColumnsUpdate"
      />

      <Button
        v-if="data?.tableMetadata"
        type="outline"
        class="h-50px"
        :href="`/${schemaId}/api/csv/${tableId}`"
        icon="Download"
        download
      >
        Download
      </Button>
    </div>
  </div>

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
            :hasRowActions="hasRowActions"
            :rowActionsWidthClass="rowActionsWidthClass"
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
          :hasRowActions="hasRowActions"
          :rowActionsWidthClass="rowActionsWidthClass"
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
              'hover:cursor-pointer': props.isEditable || isRowClickable,
            }"
            @click="onRowClick(row, $event)"
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
              v-for="column in sortedVisibleColumns"
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
            </TableCellEMX2>

            <!--
              Floating row actions pinned to the right edge of the horizontal scroll
              viewport (sticky), revealed on row hover. The reserved width doubles as
              trailing whitespace so the last data cell can be scrolled clear of the
              buttons. A matching empty header cell keeps table-fixed columns aligned.
            -->
            <td
              v-if="hasRowActions"
              class="sticky right-0 z-10 p-0 border-b group-hover:bg-hover"
              :class="rowActionsWidthClass"
            >
              <!--
                Editable rows reveal their multi-action cluster (incl. destructive
                actions) on hover to keep the table calm; a single read-only action
                can be kept persistently visible so it stays discoverable.
              -->
              <div
                class="flex h-full items-center justify-end"
                :class="
                  !isEditable && persistReadOnlyActions
                    ? ''
                    : 'invisible group-hover:visible'
                "
              >
                <div
                  class="relative flex h-full items-center gap-1 px-3 bg-table group-hover:bg-hover"
                >
                  <!--
                    Fade just left of the button bar: softens the hard edge where the
                    underlying value meets the (opaque) action bar.
                  -->
                  <div
                    class="pointer-events-none absolute inset-y-0 right-full w-12 bg-gradient-to-r from-transparent to-[var(--background-color-table)] group-hover:to-[var(--background-color-hover)]"
                  />
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
                    {{ row._rowIdString }}
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
                    {{ row._rowIdString }}
                  </Button>

                  <slot name="additional-row-actions" :row="row" />
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div
        class="sticky left-0 flex justify-center items-center py-2.5"
        v-if="status === 'success' && !rows?.length"
      >
        <TextNoResultsMessage
          class="w-full text-center"
          label="No records found"
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
    :showButton="false"
    :schemaId="props.schemaId"
    :metadata="data.tableMetadata"
    :keys="new Set(selectedRows.values())"
    v-model:visible="showDeleteMultipleModal"
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

<script setup lang="ts">
import {
  computed,
  getCurrentInstance,
  onMounted,
  onUnmounted,
  ref,
  useId,
  useSlots,
  watch,
} from "vue";
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
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import { getPrimaryKey } from "../../utils/getPrimaryKey";

import TableCellEMX2 from "./CellEMX2.vue";

import DeleteModal from "../form/DeleteModal.vue";
import EditModal from "../form/EditModal.vue";
import InputSearch from "../input/Search.vue";

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
import RowControles from "./control/RowControles.vue";
import DeleteRows from "./control/DeleteRows.vue";
import TableControlColumns from "./control/Columns.vue";
import TableEMX2Head from "./TableEMX2Head.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    isEditable?: boolean;
    useStickyHeader?: boolean;
    // Keep a read-only row's single action (e.g. view details) always visible
    // instead of revealing it on hover, so it stays discoverable.
    persistReadOnlyActions?: boolean;
  }>(),
  {
    isEditable: () => false,
    useStickyHeader: () => true,
    persistReadOnlyActions: () => false,
  }
);

const slots = useSlots();
// Whether to render the floating right-hand action column (edit/delete buttons
// and/or any custom row actions). Drives a matching empty header cell so the
// table-fixed columns stay aligned.
const hasRowActions = computed(
  () => props.isEditable || Boolean(slots["additional-row-actions"])
);

// Reserve only as much width as the rendered actions need: editable rows show
// delete + edit + the optional slot action, read-only rows show just the slot
// action, so a single fixed width would leave a large empty (highlighted) gap.
const rowActionsWidthClass = computed(() =>
  props.isEditable ? "w-40" : "w-20"
);

const emit = defineEmits<{
  (e: "view-details", payload: TableRow): void;
  (e: "rowClick", payload: TableRow): void;
}>();

// Rows are clickable only when the consumer listens for `rowClick`.
const instance = getCurrentInstance();
const isRowClickable = computed(() =>
  Boolean(instance?.vnode.props?.onRowClick)
);

function onRowClick(row: TableRow, event: MouseEvent) {
  if (!isRowClickable.value) return;
  const target = event.target as HTMLElement | null;
  // Don't hijack clicks on interactive cell content (links, buttons, checkboxes,
  // ref / ontology cells) or while the user is selecting text.
  if (
    target?.closest(
      'a, button, input, select, textarea, label, [role="button"], .text-link'
    )
  ) {
    return;
  }
  if (window.getSelection()?.toString()) return;
  emit("rowClick", row);
}

const showAddModal = ref<boolean>(false);
const showEditModal = ref<boolean>(false);
const showDeleteModal = ref<boolean>(false);
const showDeleteMultipleModal = ref<boolean>(false);
const rowDataForModal = ref();
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

export type TableRow = {
  _rowId: Record<string, columnValue>;
  _rowIdString: string;
} & Record<string, columnValue>;

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
      rows,
      count: tableData.count,
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

const rows = computed((): TableRow[] =>
  Array.isArray(data.value?.rows) ? data.value?.rows : []
);

const showDraftColumn = computed(() =>
  rows.value.some((row: TableRow) => row?.mg_draft === true)
);

const count = computed(() => data.value?.count ?? 0);

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

const allRowsSelected = computed(() => {
  return (
    rows.value.length > 0 &&
    rows.value.every((row) => selectedRows.value.has(row._rowIdString))
  );
});

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

async function toggleAllRows() {
  if (allRowsSelected.value) {
    selectedRows.value.clear();
  } else {
    rows.value.forEach((row) => {
      selectedRows.value.set(row._rowIdString, row._rowId);
    });
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.size === 1
        ? rows.value.find((row) => selectedRows.value.has(row._rowIdString))
        : null;
    if (action === "delete-selection" && singleRowSelected) {
      onShowDeleteModal(singleRowSelected);
    } else if (action === "edit-selection" && singleRowSelected) {
      onShowEditModal(singleRowSelected);
    } else if (action === "view-details" && singleRowSelected) {
      emit("view-details", singleRowSelected);
    } else if (action === "delete-selection" && selectedRows.value.size > 1) {
      showDeleteMultipleModal.value = true;
    } else if (action === "select-all-on-page") {
      rows.value.forEach((row) => {
        selectedRows.value.set(row._rowIdString, row._rowId);
      });
    } else if (action === "select-none") {
      selectedRows.value.clear();
    } else if (action === "select-drafts") {
      selectedRows.value.clear();
      rows.value.forEach((row) => {
        if (row.mg_draft === true) {
          selectedRows.value.set(row._rowIdString, row._rowId);
        }
      });
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

function handleSearchRequest(search?: unknown) {
  if (typeof search === "number") {
    settings.value.search = search.toString();
  } else if (typeof search === "string") {
    settings.value.search = search;
  } else {
    settings.value.search = "";
  }
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
