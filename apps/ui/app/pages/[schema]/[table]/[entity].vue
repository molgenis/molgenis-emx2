<script setup lang="ts">
import { createError, showError, useAsyncData } from "#app";
import { useRoute, useRouter } from "#app/composables/router";
import { computed, ref, useId } from "vue";
import type {
  IRow,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import DisplayRecord from "../../../../../tailwind-components/app/components/display/Record.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import fetchRowData, {
  RowNotFoundError,
} from "../../../../../tailwind-components/app/composables/fetchRowData";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import { useTablePermission } from "../../../../../tailwind-components/app/composables/useTablePermission";
import { DATA_NOT_FOUND_ERROR } from "../../../../../tailwind-components/app/utils/constants";
import { fetchErrorToNuxtError } from "../../../../../tailwind-components/app/utils/fetchErrorToNuxtError";
import { parseMgTableclass } from "../../../../../tailwind-components/app/utils/parseMgTableclass";
import { rowMatchesUserRole } from "../../../../../tailwind-components/app/utils/rowMatchesUserRole";
import type { cellPayload } from "../../../../../tailwind-components/types/types";
import Container from "../../../../../tailwind-components/app/components/Container.vue";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
const keys = route.query.keys as string | undefined;
let entityKeysObject: IRow = {};

const showModal = ref(false);
const cellDetailPayload = ref<cellPayload>();

try {
  if (keys) {
    entityKeysObject = JSON.parse(keys) as IRow;
  }
} catch {
  // If the query parameter is malformed JSON, fall back to an empty object
  entityKeysObject = {};
}
const { isAdmin, session } = await useSession(schemaId);

interface RecordData {
  tableMetadata: ITableMetaData;
  rowData: IRow;
  // A row loaded through its parent table carries only the parent's columns.
  viewMetadata: ITableMetaData;
  viewRowData: IRow;
}

async function fetchRecordData(): Promise<RecordData> {
  const tableMetadata = await fetchTableMetadata(schemaId, tableId);

  let rowData: IRow;
  try {
    rowData = await fetchRowData(schemaId, tableId, entityKeysObject);
  } catch (error) {
    if (error instanceof RowNotFoundError) {
      const message = `Could not find this row in table "${tableId}" of schema "${schemaId}". ${DATA_NOT_FOUND_ERROR}`;
      console.error(message, error);
      throw createError({ status: 404, message });
    }
    throw fetchErrorToNuxtError(
      error,
      `Could not load this row in table "${tableId}" of schema "${schemaId}".`
    );
  }

  const parsed = parseMgTableclass(rowData.mg_tableclass);
  if (!parsed || parsed.tableId === tableId) {
    return {
      tableMetadata,
      rowData,
      viewMetadata: tableMetadata,
      viewRowData: rowData,
    };
  }

  try {
    const subMetadata = await fetchTableMetadata(
      parsed.schemaId,
      parsed.tableId
    );
    const subRowData = await fetchRowData(
      parsed.schemaId,
      parsed.tableId,
      entityKeysObject
    );
    return {
      tableMetadata,
      rowData,
      viewMetadata: subMetadata,
      viewRowData: subRowData,
    };
  } catch (error) {
    console.error(
      `Could not load "${parsed.tableId}" for this row, showing "${tableId}" instead.`,
      error
    );
    return {
      tableMetadata,
      rowData,
      viewMetadata: tableMetadata,
      viewRowData: rowData,
    };
  }
}

const {
  data: recordData,
  error: recordError,
  refresh,
} = await useAsyncData(
  `${schemaId}/${tableId}/${keys || JSON.stringify(entityKeysObject)}`,
  fetchRecordData
);
if (recordError.value) {
  throw createError(recordError.value);
}

// Safe: the throw above guarantees recordData is populated before first render.
const tableMetadata = computed(() => recordData.value!.tableMetadata);
const rowData = computed(() => recordData.value!.rowData);
const viewMetadata = computed(() => recordData.value!.viewMetadata);
const viewRowData = computed(() => recordData.value!.viewRowData);

const showEditModal = ref(false);
const showDeleteModal = ref(false);

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}
async function afterEditClosed() {
  showEditModal.value = false;
  await refresh();
  // refresh() can fail; the computeds above assume recordData is never null, so surface it here.
  if (recordError.value) {
    showError(recordError.value);
  }
}

const { canUpdate, canDelete, isRowLevel, userRoles } = useTablePermission(
  session,
  schemaId,
  tableId,
  tableMetadata.value.tableType
);

const rowIsModifiable = computed(
  () => !isRowLevel.value || rowMatchesUserRole(rowData.value, userRoles.value)
);

const enableEditing = computed(() => canUpdate.value && rowIsModifiable.value);

const enableDeleting = computed(() => canDelete.value && rowIsModifiable.value);

function handleCellClick(event: cellPayload) {
  cellDetailPayload.value = event;
  showModal.value = true;
}
</script>

<template>
  <Container>
    <PageHeader :title="entityId" align="left">
      <template #prefix>
        <BreadCrumbs
          :align="'left'"
          :crumbs="[
            { label: schemaId, url: `/${schemaId}` },
            { label: tableMetadata.label, url: `/${schemaId}/${tableId}` },
          ]"
        />
      </template>
    </PageHeader>

    <div class="flex pb-[30px] gap-[10px] justify-end">
      <Button
        type="outline"
        icon="edit"
        @click="showEditModal = true"
        v-if="enableEditing"
        >Edit
      </Button>
      <Button
        type="outline"
        icon="trash"
        @click="showDeleteModal = true"
        v-if="enableDeleting"
      >
        Delete
      </Button>
    </div>

    <DisplayRecord
      :metadata="viewMetadata"
      :rowData="viewRowData"
      :showMgColumns="isAdmin"
      :showLegend="true"
      :showCards="true"
      :showFilter="true"
      @valueClick="handleCellClick($event)"
    />
  </Container>

  <CellDetailModal
    v-if="cellDetailPayload"
    :payload="cellDetailPayload"
    :schemaId="schemaId"
    v-model:showModal="showModal"
    @update:cellDetailPayload="cellDetailPayload = $event"
  />

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
