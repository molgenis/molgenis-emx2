<template>
  <div class="flex pb-[30px] justify-between">
    <InputSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      v-model="settings.search"
      @update:modelValue="handleSearchRequest"
      :placeholder="`Search ${props.tableId}`"
      id="search-input"
    />

    <div class="flex gap-[10px]">
      <EditModal
        v-if="props.isEditable && data?.tableMetadata"
        :metadata="data.tableMetadata"
        :schemaId="props.schemaId"
        v-slot="{ setVisible }"
        @update:added="afterRowAdded"
      >
        <Button type="primary" icon="add-circle" @click="setVisible"
          >Add {{ tableId }}
        </Button>
      </EditModal>

      <TableControlColumns
        :columns="columns"
        @update:columns="handleColumnsUpdate"
      />
    </div>
  </div>

  <div
    class="relative overflow-auto rounded-b-theme border border-theme border-color-theme"
  >
    <div class="overflow-x-auto overscroll-x-contain bg-table rounded-t-3px">
      <table ref="table" class="text-left w-full table-fixed">
        <thead>
          <tr>
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
              :class="{
                'w-60 lg:w-full': columns.length <= 5,
                'w-60': columns.length > 5,
              }"
            >
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
              v-if="showDraftColumn"
              class="text-table-row group-hover:bg-hover"
            >
              <DraftLabel v-if="row?.mg_draft === true" type="inline" />
            </TableCellEMX2>

            <TableCellEMX2
              v-for="(column, colIndex) in sortedVisibleColumns"
              class="text-table-row group-hover:bg-hover"
              :class="{
                'w-60 lg:w-full': columns.length <= 5,
                'w-60': columns.length > 5,
                'h-11': !row[column.id] || row[column.id] === '',
              }"
              :scope="column.key === 1 ? 'row' : null"
              :metadata="column"
              :data="row[column.id]"
              @cellClicked="handleCellClick($event, column, row)"
            >
              <template #row-actions v-if="colIndex === 0 && props.isEditable">
                <div
                  class="absolute left-0 h-10 -mt-2 w-[100px] z-10 text-table-row bg-hover group-hover:bg-hover invisible group-hover:visible border-none group-hover:flex flex-row items-center justify-start flex-nowrap gap-1"
                >
                  <Button
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

  <TableModalRef
    :id="`table-emx2-${schemaId}-${tableId}-modal-ref`"
    v-if="showModal && refTableRow && refTableColumn"
    v-model:visible="showModal"
    :metadata="refTableColumn"
    :row="refTableRow"
    :schema="schemaId"
    :sourceTableId="refSourceTableId"
    :showDataOwner="false"
  />

  <DeleteModal
    v-if="data?.tableMetadata && rowDataForModal"
    :showButton="false"
    :schemaId="props.schemaId"
    :metadata="data.tableMetadata"
    :formValues="rowDataForModal"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
  />

  <EditModal
    v-if="data?.tableMetadata && rowDataForModal"
    :showButton="false"
    :schemaId="props.schemaId"
    :metadata="data.tableMetadata"
    :formValues="rowDataForModal"
    v-model:visible="showEditModal"
    @update:updated="afterRowUpdated"
  />
</template>

<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import type {
  IRow,
  IColumn,
  IRefColumn,
  columnValue,
} from "../../../../metadata-utils/src/types";
import type {
  ITableSettings,
  RefPayload,
  sortDirection,
} from "../../../types/types";
import { sortColumns } from "../../utils/sortColumns";

import { useAsyncData } from "#app/composables/asyncData";
import { fetchTableData, fetchTableMetadata } from "#imports";

import TableCellEMX2 from "./CellEMX2.vue";
import TableHeadCell from "./TableHeadCell.vue";

import EditModal from "../form/EditModal.vue";
import DeleteModal from "../form/DeleteModal.vue";
import TableModalRef from "./modal/TableModalRef.vue";
import InputSearch from "../input/Search.vue";

import Button from "../Button.vue";
import Pagination from "../Pagination.vue";
import TableControlColumns from "./control/Columns.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import TableHeaderAction from "./TableHeaderAction.vue";
import DraftLabel from "../label/DraftLabel.vue";

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

const showDeleteModal = ref<boolean>(false);
const showEditModal = ref<boolean>(false);
const rowDataForModal = ref();
const showModal = ref(false);
const refTableRow = ref<IRow>();
const refTableColumn = ref<IRefColumn>();
// initially set to the current tableId
const refSourceTableId = ref<string>(props.tableId);
const columns = ref<IColumn[]>([]);

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: 10,
    orderby: { column: "", direction: "ASC" },
    search: "",
  }),
});

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

    return {
      tableMetadata,
      tableData,
    };
  }
);

const rows = computed(() => {
  if (!data.value?.tableData) return [];

  return data.value.tableData.rows;
});

const showDraftColumn = computed(() =>
  rows.value.some((row) => row?.mg_draft === true)
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

function handleColumnsUpdate(newColumns: IColumn[]) {
  columns.value = newColumns;
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

function handleCellClick(
  event: RefPayload,
  column: IColumn,
  row: Record<string, any>
) {
  refTableRow.value = event.data;
  refTableColumn.value =
    column.columnType === "REF"
      ? (column as IRefColumn)
      : (column as IRefColumn); // todo other types of column

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

function afterRowAdded() {
  // todo reset filters and search, goto page with added item, flash row with add item
  refresh();
}

function afterRowUpdated() {
  refresh();
}

function afterRowDeleted() {
  // maybe notify user, and do more stuff
  refresh();
}
</script>
