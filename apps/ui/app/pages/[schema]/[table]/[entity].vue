<script setup lang="ts">
import { useAsyncData } from "#app";
import { useRoute, useRouter } from "#app/composables/router";
import { computed, ref, useId } from "vue";
import type { IRow } from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import DisplayRecord from "../../../../../tailwind-components/app/components/display/Record.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import resolveSubclassRecord from "../../../../../tailwind-components/app/composables/resolveSubclassRecord";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import { useTablePermission } from "../../../../../tailwind-components/app/composables/useTablePermission";
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

const tableMetadata = await fetchTableMetadata(schemaId, tableId);
const { data: rowData, refresh } = await useAsyncData(
  keys || JSON.stringify(entityKeysObject),
  () => fetchRowData(schemaId, tableId, entityKeysObject)
);

const showEditModal = ref(false);
const showDeleteModal = ref(false);

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}
async function afterEditClosed() {
  showEditModal.value = false;
  await refresh();
  await resolveView();
}

const { canUpdate, canDelete, isRowLevel, userRoles } = useTablePermission(
  session,
  schemaId,
  tableId,
  tableMetadata.tableType
);

const rowIsModifiable = computed(
  () =>
    !isRowLevel.value ||
    (!!rowData.value && rowMatchesUserRole(rowData.value, userRoles.value))
);

const enableEditing = computed(() => canUpdate.value && rowIsModifiable.value);

const enableDeleting = computed(() => canDelete.value && rowIsModifiable.value);

// A subclass row loaded via its parent table only carries the parent's columns; resolve its
// own table so the view renders every field the row has, without changing the route or edit/delete.
const viewMetadata = ref(tableMetadata);
const viewRowData = ref(rowData.value);

async function resolveView() {
  const subclass = await resolveSubclassRecord(
    schemaId,
    tableId,
    rowData.value,
    entityKeysObject,
    fetchTableMetadata,
    fetchRowData
  );
  viewMetadata.value = subclass ? subclass.tableMetadata : tableMetadata;
  viewRowData.value = subclass ? subclass.row : rowData.value;
}
await resolveView();

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
      :showMenu="true"
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
