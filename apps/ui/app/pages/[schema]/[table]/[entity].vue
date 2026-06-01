<script setup lang="ts">
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import DetailView from "../../../../../tailwind-components/app/components/display/DetailView.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import { computed, nextTick, ref, useId } from "vue";
import { useAsyncData } from "#app";
import Modal from "../../../../../tailwind-components/app/components/Modal.vue";
import TableCellDetailRef from "../../../../../tailwind-components/app/components/table/cellDetail/TableCellDetailRef.vue";
import {
  toRefColumn,
  toRefColumnValue,
} from "../../../../../tailwind-components/app/utils/typeUtils";
import {
  isArrayLikeDetail,
  isRefLikeDetail,
} from "../../../../../tailwind-components/app/utils/refUtils";
import type {
  cellPayload,
  ColumnPayload,
  ListPayload,
  RefPayload,
} from "../../../../../tailwind-components/types/types";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
const keys = route.query.keys as string | undefined;
let rowId: IRow = {};

try {
  if (keys) {
    rowId = JSON.parse(keys) as IRow;
  }
} catch {
  rowId = {};
}

const { isAdmin, session } = await useSession(schemaId);

const tableMetadata = await fetchTableMetadata(schemaId, tableId);
const { data: rowData, refresh } = await useAsyncData(
  keys || JSON.stringify(rowId),
  () => fetchRowData(schemaId, tableId, rowId)
);

const showEditModal = ref(false);
const showDeleteModal = ref(false);
const recordViewKey = ref(0);

function afterEditClosed() {
  showEditModal.value = false;
  refresh();
  recordViewKey.value++;
}

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}

const enableEditing = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});

const enableDeleting = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});

const showModal = ref(false);
const cellDetailSchemaId = ref<string>();
const cellDetailColumn = ref<IColumn>();
const cellDetailSubtitle = ref<string>();
const cellDetailValue = ref<columnValue>();

const showRefDetailModal = computed(() => {
  return (
    cellDetailColumn.value &&
    isRefLikeDetail(cellDetailColumn.value) &&
    !isArrayLikeDetail(cellDetailColumn.value) &&
    showModal.value
  );
});

function handleCellClick(event: cellPayload) {
  const column = event.metadata as IColumn;
  cellDetailSubtitle.value = column.label;
  cellDetailColumn.value = column;
  cellDetailSchemaId.value = column.refSchemaId ?? schemaId;
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
  cellDetailSchemaId.value = columnMetadata.refSchemaId ?? schemaId;
  cellDetailValue.value = event.data as columnValue;

  showModal.value = true;
}
</script>

<template>
  <DetailView
    :key="recordViewKey"
    :schema-id="schemaId"
    :table-id="tableId"
    :row-id="rowId"
    @valueClick="handleCellClick"
  >
    <template #header>
      <PageHeader :title="entityId">
        <template #prefix>
          <BreadCrumbs
            :crumbs="[
              { label: schemaId, url: `/${schemaId}` },
              { label: tableId, url: `/${schemaId}/${tableId}` },
            ]"
          />
        </template>
      </PageHeader>

      <div class="flex pb-[30px] gap-[10px] justify-end">
        <Button
          v-if="enableEditing"
          type="outline"
          icon="edit"
          @click="showEditModal = true"
          >Edit
        </Button>
        <Button
          v-if="enableDeleting"
          type="outline"
          icon="trash"
          @click="showDeleteModal = true"
          >Delete
        </Button>
      </div>
    </template>
  </DetailView>

  <Modal
    type="right"
    v-model:visible="showModal"
    :title="cellDetailSubtitle"
    @closed="showModal = false"
  >
    <TableCellDetailRef
      v-if="cellDetailColumn && showRefDetailModal"
      :metadata="toRefColumn(cellDetailColumn)"
      :columnValue="toRefColumnValue(cellDetailValue)"
      :schema="cellDetailSchemaId ?? schemaId"
      :showDataOwner="false"
      @onRefClick="handleDetailRefClick"
    />
    <template
      v-else-if="
        cellDetailValue &&
        cellDetailColumn &&
        isArrayLikeDetail(cellDetailColumn)
      "
    >
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
    v-if="tableMetadata && rowData && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="rowData"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="tableMetadata && rowData && showEditModal"
    :key="`edit-modal-${useId()}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="rowData"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
