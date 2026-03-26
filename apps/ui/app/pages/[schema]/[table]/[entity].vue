<script setup lang="ts">
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type { IRow } from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import Emx2RecordView from "../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
import DeleteModal from "../../../../../tailwind-components/app/components/form/DeleteModal.vue";
import fetchTableMetadata from "../../../../../tailwind-components/app/composables/fetchTableMetadata";
import fetchRowData from "../../../../../tailwind-components/app/composables/fetchRowData";
import { computed, ref, useId } from "vue";
import { useAsyncData } from "#app";

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

function afterRowDeleted() {
  router.push(`/${schemaId}/${tableId}`);
}

function afterEditClosed() {
  showEditModal.value = false;
  refresh();
  recordViewKey.value++;
}

const enableEditing = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});

const enableDeleting = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});
</script>

<template>
  <Emx2RecordView
    :key="recordViewKey"
    :schema-id="schemaId"
    :table-id="tableId"
    :row-id="rowId"
  >
    <template #header>
      <PageHeader :title="entityId" align="left">
        <template #prefix>
          <BreadCrumbs
            :align="'left'"
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
  </Emx2RecordView>

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
