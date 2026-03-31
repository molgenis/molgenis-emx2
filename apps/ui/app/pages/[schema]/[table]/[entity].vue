<script setup lang="ts">
import { useRoute, useRouter } from "#app/composables/router";
import { useSession } from "../../../../../tailwind-components/app/composables/useSession";
import type { IRow } from "../../../../../metadata-utils/src/types";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../../../tailwind-components/app/components/Button.vue";
import DetailView from "../../../../../tailwind-components/app/components/display/DetailView.vue";
import EditModal from "../../../../../tailwind-components/app/components/form/EditModal.vue";
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
const recordViewKey = ref(0);

function afterEditClosed() {
  showEditModal.value = false;
  refresh();
  recordViewKey.value++;
}

const enableEditing = computed(() => {
  return session.value?.roles?.[schemaId]?.includes("Editor") || isAdmin.value;
});
</script>

<template>
  <DetailView
    :key="recordViewKey"
    :schema-id="schemaId"
    :table-id="tableId"
    :row-id="rowId"
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
      </div>
    </template>
  </DetailView>

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
