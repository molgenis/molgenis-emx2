<script setup lang="ts">
import { createError, showError, useAsyncData } from "#app";
import { useRoute, useRouter } from "#app/composables/router";
import { computed, ref, useId, watch } from "vue";
import type { IRow } from "../../../../../metadata-utils/src/types";
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

async function fetchUrlRow(): Promise<IRow> {
  try {
    return await fetchRowData(schemaId, tableId, entityKeysObject);
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
}

async function fetchRecordData() {
  const urlTable = await fetchTableMetadata(schemaId, tableId);
  const urlRow = await fetchUrlRow();

  // A row loaded through its parent table carries only the parent's columns.
  function fallbackToUrlTable() {
    return { urlTable, urlRow, recordTable: urlTable, recordRow: urlRow };
  }

  const parsed = parseMgTableclass(urlRow.mg_tableclass);
  if (!parsed || parsed.tableId === tableId) {
    return fallbackToUrlTable();
  }

  try {
    const recordTable = await fetchTableMetadata(
      parsed.schemaId,
      parsed.tableId
    );
    const recordRow = await fetchRowData(
      parsed.schemaId,
      parsed.tableId,
      entityKeysObject
    );
    return { urlTable, urlRow, recordTable, recordRow };
  } catch (error) {
    console.error(
      `Could not load "${parsed.tableId}" for this row, showing "${tableId}" instead.`,
      error
    );
    return fallbackToUrlTable();
  }
}

// useAsyncData resets data to undefined when a later refresh() fails; keep the last
// good page state so a failed refresh cannot leave the page reading null data.
let lastGoodRecordData: Awaited<ReturnType<typeof fetchRecordData>> | undefined;

const {
  data: recordData,
  error: recordError,
  refresh,
} = await useAsyncData(
  `${schemaId}/${tableId}/${keys || JSON.stringify(entityKeysObject)}`,
  fetchRecordData,
  { default: () => lastGoodRecordData }
);
if (recordError.value) {
  throw createError(recordError.value);
}
watch(
  recordData,
  (value) => {
    if (value) lastGoodRecordData = value;
  },
  { immediate: true }
);
watch(recordError, (error) => {
  if (error) showError(error);
});

// Safe: the throw above guarantees recordData is populated before first render.
const urlTable = computed(() => recordData.value!.urlTable);
const urlRow = computed(() => recordData.value!.urlRow);
const recordTable = computed(() => recordData.value!.recordTable);
const recordRow = computed(() => recordData.value!.recordRow);

const showEditModal = ref(false);
const showDeleteModal = ref(false);

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}
function afterEditClosed() {
  showEditModal.value = false;
  refresh();
}

const { canUpdate, canDelete, isRowLevel, userRoles } = useTablePermission(
  session,
  schemaId,
  tableId,
  urlTable.value.tableType
);

const rowIsModifiable = computed(
  () => !isRowLevel.value || rowMatchesUserRole(urlRow.value, userRoles.value)
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
            { label: urlTable.label, url: `/${schemaId}/${tableId}` },
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
      :metadata="recordTable"
      :rowData="recordRow"
      :showMgColumns="isAdmin"
      :showLegend="true"
      :showCards="true"
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
    v-if="urlTable && urlRow && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="urlTable"
    :formValues="urlRow"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="urlTable && urlRow && showEditModal"
    :key="`edit-modal-${useId()}`"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="urlTable"
    :formValues="urlRow"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
