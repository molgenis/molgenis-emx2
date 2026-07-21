<script setup lang="ts">
import { useAsyncData } from "#app";
import { useRoute, useRouter } from "#app/composables/router";
import { computed, ref, useId } from "vue";
import type { IRow } from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import DetailView from "../../../../../tailwind-components/app/components/display/DetailView.vue";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type { cellPayload } from "../../../../../tailwind-components/types/types";

const route = useRoute();
const router = useRouter();
const schemaId = route.params.schema as string;
const tableId = route.params.table as string;
const entityId = route.params.entity as string;
const keys = route.query.keys as string | undefined;
let rowId: IRow = {};

const showModal = ref(false);
const cellDetailPayload = ref<cellPayload>();

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

const editModalKey = `edit-modal-${useId()}`;

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

function handleCellClick(event: cellPayload) {
  cellDetailPayload.value = event;
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
    :key="editModalKey"
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
