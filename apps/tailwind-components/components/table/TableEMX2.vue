<template>
  <div class="flex pb-[30px] justify-between">
    <FilterSearch
      class="w-3/5 xl:w-2/5 2xl:w-1/5"
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    >
    </FilterSearch>

    <div class="flex gap-[10px]">
      <AddModal
        v-if="props.isEditable && data?.tableMetadata"
        :metadata="data.tableMetadata"
        :schemaId="props.schemaId"
        v-slot="{ setVisible }"
        @update:added="afterRowAdded"
      >
        <Button type="primary" icon="add-circle" @click="setVisible"
          >Add {{ tableId }}</Button
        >
      </AddModal>

      <TableControlColumns
        :columns="columns"
        @update:columns="handleColumnsUpdate"
      />
    </div>
  </div>

  <div class="overflow-auto rounded-b-theme">
    <div class="overflow-x-auto overscroll-x-contain bg-table rounded-t-theme">
      <table
        class="text-left table-fixed w-full border border-theme border-color-theme"
      >
        <thead>
          <tr>
            <th
              v-for="(column, colIndex) in sortedVisibleColumns"
              class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left overflow-hidden whitespace-nowrap align-middle"
              :style="`width: ${column.width}px`"
              :ariaSort="
                settings.orderby.column === column.id
                  ? mgAriaSortMappings[settings.orderby.direction]
                  : 'none'
              "
              scope="col"
            >
              <span
                class="whitespace-nowrap max-w-60 w-64 overflow-hidden inline-block"
              >
                <button
                  @click="handleSortRequest(column.id)"
                  class="overflow-ellipsis whitespace-nowrap max-w-56 overflow-hidden inline-block text-left text-table-column-header font-normal align-middle"
                >
                  {{ column.label }}
                </button>
                <ArrowUp
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'ASC'
                  "
                  class="w-4 h-4 inline-block ml-1 text-table-column-header font-normal"
                />
                <ArrowDown
                  v-if="
                    column.id === settings.orderby.column &&
                    settings.orderby.direction === 'DESC'
                  "
                  class="w-4 h-4 inline-block ml-1 text-table-column-header font-normal"
                />
              </span>
            </th>
          </tr>
        </thead>
        <tbody
          class="mb-3 [&_tr:last-child_td]:border-none [&_tr:last-child_td]:mb-5"
        >
          <tr
            v-for="row in rows"
            class="static hover:bg-blue-50 group h-4"
            :class="{ 'hover:cursor-pointer': props.isEditable }"
          >
            <TableCellTypesEMX2
              v-for="column in sortedVisibleColumns"
              class="text-table-row"
              :scope="column.key === 1 ? 'row' : null"
              :metaData="column"
              :data="row[column.id]"
              @mousedown="onMouseDown($event, column)"
              @mouseup="onMouseUp"
              @mousemove="mouseElem && onMouseMove($event, column)"
            >
              <div
                v-if="isEditable && index === 0"
                class="flex items-center gap-1 flex-none invisible group-hover:visible h-4 py-6 px-4 absolute right-7 bg-blue-50"
              >
                <DeleteModal
                  v-if="data?.tableMetadata"
                  :schemaId="props.schemaId"
                  :metadata="data.tableMetadata"
                  :formValues="row"
                  v-slot="{ setVisible }"
                  @update:deleted="afterRowDeleted"
                >
                  <Button
                    :icon-only="true"
                    type="inline"
                    icon="trash"
                    size="small"
                    label="delete"
                    @click="setVisible"
                  />
                </DeleteModal>
                <EditModal
                  v-if="data?.tableMetadata"
                  :schemaId="props.schemaId"
                  :metadata="data.tableMetadata"
                  :formValues="row"
                  v-slot="{ setVisible }"
                  @update:updated="afterRowUpdated"
                >
                  <Button
                    :icon-only="true"
                    type="inline"
                    icon="edit"
                    size="small"
                    label="edit"
                    @click="setVisible"
                  />
                </EditModal>
              </div>
            </TableCellTypesEMX2>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <Pagination
    class="pt-[30px] pb-[30px]"
    :current-page="settings.page"
    :totalPages="Math.ceil(count / settings.pageSize)"
    @update="handlePagingRequest($event)"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn } from "../../../metadata-utils/src/types";
import type { ITableSettings, sortDirection } from "../../types/types";
import { sortColumns } from "../../utils/sortColumns";

import { useAsyncData } from "#app/composables/asyncData";
import { fetchTableData, fetchTableMetadata } from "#imports";
import AddModal from "../form/AddModal.vue";
import EditModal from "../form/EditModal.vue";
import DeleteModal from "../form/DeleteModal.vue";

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

const settings = defineModel<ITableSettings>("settings", {
  required: false,
  default: () => ({
    page: 1,
    pageSize: 10,
    orderby: { column: "", direction: "ASC" },
    search: "",
  }),
});

const mgAriaSortMappings = {
  ASC: "ascending",
  DESC: "descending",
};

const colWith = ref(256);

// use useAsyncData to have control of status, error, and refresh
const { data, status, error, refresh, clear } = useAsyncData(
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

const count = computed(() => data.value?.tableData?.count ?? 0);

interface ISizedColumn extends IColumn {
  width: number;
}

const columns = ref<ISizedColumn[]>([]);

watch(
  () => data.value?.tableMetadata,
  (newMetadata) => {
    if (newMetadata) {
      columns.value = newMetadata.columns
        .filter((c) => !c.id.startsWith("mg") && c.columnType !== "HEADING")
        .map((column) => {
          return {
            ...column,
            width: 256,
          };
        });
    }
  },
  { immediate: true }
);

const sortedVisibleColumns = computed(() => {
  const visibleColumns = columns.value.filter(
    (column) => column.visible !== "false"
  );
  return sortColumns(visibleColumns);
});

function handleColumnsUpdate(newColumns: ISizedColumn[]) {
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

const mouseElem = ref<HTMLElement | null>(null);
const dragStart = ref(0);
const dragColumnIndex = ref(0);

function onMouseDown(event: MouseEvent) {
  mouseElem.value = event.target as HTMLElement;
  dragStart.value = event.pageX;

  console.log("onMouseDown", mouseElem.value);
}

function onMouseUp(event: MouseEvent) {
  mouseElem.value = null;
  dragColumnIndex.value = 0;
  dragStart.value = 0;
  console.log("onMouseUp", event.pageX);
}

function onMouseMove(event: MouseEvent, column: ISizedColumn) {
  if (mouseElem.value && dragStart.value) {
    const diff = dragStart.value - event.pageX;
    console.log("onMouseMove", diff);

    if (diff > 0) {
      column.width = column.width - 1;
    } else {
      column.width = column.width + 1;
    }
  }
}
</script>
