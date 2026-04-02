<template>
  <div class="flex pb-[30px] justify-between">
    <RowControles
      :number-of-selected-rows="numberOfSelectedRows"
      :can-edit="props.isEditable"
      @row-action="handleRowAction"
    />
    <InputSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      v-model="settings.search"
      @update:modelValue="handleSearchRequest"
      :placeholder="`Search ${props.tableId}`"
      id="search-input"
    />

    <div class="flex gap-[10px]">
      <Button
        v-if="props.isEditable && data?.tableMetadata"
        type="primary"
        icon="add-circle"
        @click="onAddRowClicked"
      >
        Add {{ tableId }}
      </Button>

      <TableControlColumns
        :columns="columns"
        @update:columns="handleColumnsUpdate"
      />
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

    <div class="overflow-x-auto overscroll-x-contain bg-table rounded-t-3px">
      <table ref="table" class="text-left w-full table-fixed">
        <thead>
          <tr>
            <TableHeadCell class="sticky left-0 bg-table z-20 w-12">
              <div class="flex justify-center items-center">
                <Checkbox @change="toggleAllRows" />
              </div>
            </TableHeadCell>
            <TableHeadCell v-if="showDraftColumn" class="w-24 lg:w-28">
              <TableHeaderAction
                :column="{ id: 'mg_draft', label: 'Draft' }"
                :schemaId="schemaId"
                :tableId="tableId"
                :settings="settings"
                @sort-requested="handleSortRequest"
              />
            </TableHeadCell>
            <TableHeadCell
              v-for="column in sortedVisibleColumns"
              :style="{
                width: columnWidths[column.id] + 'px',
                userSelect: isResizing ? 'none' : 'auto',
              }"
              class="relative group"
            >
              <div
                class="absolute right-0 top-0 h-full w-4 cursor-col-resize group"
                @mousedown.stop="startResize($event, column.id)"
              >
                <div
                  class="absolute right-0 top-0 h-full w-[2px] bg-transparent hover:bg-button-primary"
                />
              </div>
              <TableHeaderAction
                :column="column"
                :schemaId="schemaId"
                :tableId="tableId"
                :settings="settings"
                @sort-requested="handleSortRequest"
              />
            </TableHeadCell>
          </tr>
        </thead>
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
                  :model-value="selectedRows.has(row._rowId)"
                  @update:model-value="toggleRowSelection(row._rowId)"
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
                'h-11': !row[column.id] || row[column.id] === '',
              }"
              :scope="column.key === 1 ? 'row' : null"
              :metadata="column"
              :data="row[column.id]"
              @cellClicked="handleCellClick($event, column)"
            >
              <template #row-actions v-if="colIndex === 0">
                <div
                  class="absolute left-12 h-10 -mt-2 z-10 text-table-row bg-inherit group-hover:bg-hover invisible group-hover:visible border-none group-hover:flex flex-row items-center justify-start flex-nowrap gap-1"
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
                    {{ JSON.stringify(row._rowId) }}
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
                    {{ JSON.stringify(row._rowId) }}
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
        v-if="!rows"
      >
        <TextNoResultsMessage
          class="w-full text-center"
          label="No records found"
        />
      </div>
    </div>
  </div>

  <Pagination
    v-if="count > settings.pageSize"
    class="pt-[30px] pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    :jump-to-edge="true"
    @update="handlePagingRequest($event)"
  />

  <Modal
    type="right"
    v-model:visible="showModal"
    :title="cellDetailSubtitle"
    @closed="showModal = false"
  >
    <TableCellDetailRef
      v-if="
        cellDetailColumn && isRefLikeDetail && !isArrayLikeDetail && showModal
      "
      :metadata="toRefColumn(cellDetailColumn)"
      :columnValue="toRefColumnValue(cellDetailValue)"
      :schema="cellDetailSchemaId ?? schemaId"
      :showDataOwner="false"
      @onRefClick="handleDetailRefClick"
    />
    <template v-else-if="cellDetailValue && isArrayLikeDetail">
      <ul>
        <li v-for="(item, index) in cellDetailValue" :key="index">
          <TableCellDetailRef
            v-if="cellDetailColumn"
            :metadata="toRefColumn(cellDetailColumn)"
            :columnValue="toRefColumnValue(item as columnValue)"
            :schema="cellDetailSchemaId ?? schemaId"
            :showDataOwner="false"
            @onRefClick="handleDetailRefClick"
          />
        </li>
      </ul>
    </template>
  </Modal>

  <DeleteModal
    v-if="data?.tableMetadata && rowDataForModal"
    :showButton="false"
    :schemaId="props.schemaId"
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
    :keys="selectedRows"
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
import { computed, nextTick, ref, useId, watch } from "vue";
import type {
  IRow,
  IColumn,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type {
  cellPayload,
  ColumnPayload,
  ITableSettings,
  ListPayload,
  RefPayload,
  sortDirection,
} from "../../../types/types";
import { sortColumns } from "../../utils/sortColumns";

import { useAsyncData } from "#app/composables/asyncData";
import { fetchTableData, fetchTableMetadata, getPrimaryKey } from "#imports";

import TableCellEMX2 from "./CellEMX2.vue";
import TableHeadCell from "./TableHeadCell.vue";

import EditModal from "../form/EditModal.vue";
import DeleteModal from "../form/DeleteModal.vue";
import Modal from "../Modal.vue";
import InputSearch from "../input/Search.vue";

import Button from "../Button.vue";
import Pagination from "../Pagination.vue";
import TableControlColumns from "./control/Columns.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import TableHeaderAction from "./TableHeaderAction.vue";
import DraftLabel from "../label/DraftLabel.vue";
import Checkbox from "../input/Checkbox.vue";
import { useColumnResize } from "../../composables/useColumnResize";
import TableCellDetailRef from "./cellDetail/TableCellDetailRef.vue";
import { toRefColumn, toRefColumnValue } from "../../utils/typeUtils";
import RowControles from "./control/RowControles.vue";
import DeleteRows from "./control/DeleteRows.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    isEditable?: boolean;
  }>(),
  {
    isEditable: () => false,
  }
);

const emit = defineEmits<{
  (e: "view-details", payload: TableRow): void;
}>();

const showAddModal = ref<boolean>(false);
const showEditModal = ref<boolean>(false);
const showDeleteModal = ref<boolean>(false);
const showDeleteMultipleModal = ref<boolean>(false);
const rowDataForModal = ref();
const showModal = ref(false);

const cellDetailSchemaId = ref<string>();
const cellDetailColumn = ref<IColumn>();
const cellDetailSubtitle = ref<string>();
const cellDetailValue = ref<columnValue>();
const columns = ref<IColumn[]>([]);
const selectedRows = ref<Set<Record<string, columnValue>>>(new Set());

const tableContainer = ref<HTMLElement | null>(null);

const { columnWidths, guideX, startResize, setInitialWidths, isResizing } =
  useColumnResize(tableContainer);

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: 10,
    orderby: { column: "", direction: "ASC" },
    search: "",
  }),
});

export type TableRow = {
  _rowId: Record<string, columnValue>;
} & Record<string, columnValue>;

const { data, refresh } = useAsyncData(
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
        return {
          ...row,
          _rowId: await getPrimaryKey(row, props.tableId, props.schemaId),
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

const rows = computed(() =>
  Array.isArray(data.value?.rows) ? data.value?.rows : []
);

const showDraftColumn = computed(() =>
  rows.value.some((row) => row?.mg_draft === true)
);

const count = computed(() => data.value?.count ?? 0);

watch(
  () => data.value?.tableMetadata,
  (newMetadata) => {
    if (newMetadata) {
      columns.value = newMetadata.columns.filter(
        (c) =>
          !c.id.startsWith("mg") &&
          !["HEADING", "SECTION"].includes(c.columnType)
      );
    }
  },
  { immediate: true }
);

const sortedVisibleColumns = computed(() => {
  const visibleColumns = columns.value.filter(
    (column: IColumn) => column.visible !== "false"
  );
  return sortColumns(visibleColumns);
});

const allRowsSelected = computed(() => {
  return (
    rows.value.length > 0 &&
    rows.value.every((row) => selectedRows.value.has(row._rowId))
  );
});

const numberOfSelectedRows = computed(() => selectedRows.value.size);

function handleColumnsUpdate(newColumns: IColumn[]) {
  columns.value = newColumns;
}

function toggleRowSelection(rowId: Record<string, columnValue>) {
  if (selectedRows.value.has(rowId)) {
    selectedRows.value.delete(rowId);
  } else {
    selectedRows.value.add(rowId);
  }
}

async function toggleAllRows() {
  if (allRowsSelected.value) {
    selectedRows.value.clear();
  } else {
    rows.value.forEach((row) => {
      selectedRows.value.add(row._rowId);
    });
  }
}

function handleRowAction(payload: { action: string }) {
  if ("action" in payload) {
    const action = payload.action;
    const singleRowSelected =
      selectedRows.value.size === 1
        ? rows.value.find((row) => selectedRows.value.has(row._rowId))
        : null;
    if (action === "delete-selection" && singleRowSelected) {
      onShowDeleteModal(singleRowSelected);
    } else if (action === "edit-selection" && singleRowSelected) {
      onShowEditModal(singleRowSelected);
    } else if (action === "view-details" && singleRowSelected) {
      emit("view-details", singleRowSelected);
    } else if (action === "delete-selection" && selectedRows.value.size > 1) {
      showDeleteMultipleModal.value = true;
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

function handlePagingRequest(page: number) {
  settings.value.page = page;
  refresh();
}

function handleCellClick(event: cellPayload, column: IColumn) {
  cellDetailSubtitle.value = column.label;
  cellDetailColumn.value = column;
  cellDetailSchemaId.value = column.refSchemaId ?? props.schemaId;
  cellDetailValue.value = event.data as columnValue;
  showModal.value = true;
}

async function handleDetailRefClick(
  event: RefPayload | ColumnPayload | ListPayload
) {
  showModal.value = false;
  await nextTick();

  const columnMetadata = event.metadata;

  cellDetailSubtitle.value = columnMetadata.label;
  cellDetailColumn.value = columnMetadata;
  cellDetailSchemaId.value = columnMetadata.refSchemaId ?? props.schemaId;

  cellDetailValue.value = event.data as columnValue;

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

async function afterRowAdded() {
  // todo reset filters and search, goto page with added item, flash row with add item
  await refresh();
}

async function afterRowUpdated() {
  await refresh();
}

async function afterClose() {
  await refresh();
}

async function afterRowDeleted() {
  // maybe notify user, and do more stuff
  await refresh();
}

const isRefLikeDetail = computed(() => {
  const type = cellDetailColumn.value?.columnType;
  return (
    type === "REF" ||
    type === "RADIO" ||
    type === "CHECKBOX" ||
    type === "SELECT" ||
    type === "ONTOLOGY" ||
    type === "REFBACK" ||
    type === "MULTISELECT"
  );
});

const isArrayLikeDetail = computed(() => {
  const type = cellDetailColumn.value?.columnType;
  return (
    type?.endsWith("_ARRAY") || type === "MULTISELECT" || type === "CHECKBOX"
  );
});
</script>
