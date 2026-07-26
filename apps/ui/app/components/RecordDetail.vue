<script setup lang="ts">
import { useRouter } from "#app/composables/router";
import { computed, ref, useId } from "vue";
import type { IRow, recordValue } from "../../../metadata-utils/src/types";
import BreadCrumbs from "../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import DeleteModal from "../../../tailwind-components/app/components/form/DeleteModal.vue";
import EditModal from "../../../tailwind-components/app/components/form/EditModal.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import DetailView from "../../../tailwind-components/app/components/display/DetailView.vue";
import fetchTableMetadata from "../../../tailwind-components/app/composables/fetchTableMetadata";
import { useSession } from "../../../tailwind-components/app/composables/useSession";
import type {
  cellPayload,
  Crumb,
} from "../../../tailwind-components/types/types";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  rowId: IRow;
  row: recordValue | null;
  title: string;
  crumbs: Crumb[];
}>();

const emit = defineEmits<{
  (event: "rowChanged"): void;
}>();

const router = useRouter();

const showModal = ref(false);
const cellDetailPayload = ref<cellPayload>();
const showEditModal = ref(false);
const showDeleteModal = ref(false);
const recordViewKey = ref(0);

const editModalKey = `edit-modal-${useId()}`;

const { isAdmin, session } = await useSession(props.schemaId);
const tableMetadata = await fetchTableMetadata(props.schemaId, props.tableId);

function afterEditClosed() {
  showEditModal.value = false;
  recordViewKey.value++;
  emit("rowChanged");
}

function afterRowDeleted() {
  router.push(`/${props.schemaId}/${props.tableId}`);
}

const canModifyRecord = computed(() => {
  return (
    session.value?.roles?.[props.schemaId]?.includes("Editor") || isAdmin.value
  );
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
      <PageHeader :title="title">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
      </PageHeader>

      <div class="flex pb-7.5 gap-2.5 justify-end">
        <Button
          v-if="canModifyRecord"
          type="outline"
          icon="edit"
          @click="showEditModal = true"
          >Edit
        </Button>
        <Button
          v-if="canModifyRecord"
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
    v-if="tableMetadata && row && showDeleteModal"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="row"
    v-model:visible="showDeleteModal"
    @update:deleted="afterRowDeleted"
    @update:cancelled="showDeleteModal = false"
  />

  <EditModal
    v-if="tableMetadata && row && showEditModal"
    :key="editModalKey"
    :showButton="false"
    :schemaId="schemaId"
    :metadata="tableMetadata"
    :formValues="row"
    :isInsert="false"
    v-model:visible="showEditModal"
    @update:cancelled="afterEditClosed"
    @update:added="afterEditClosed"
    @update:edited="afterEditClosed"
  />
</template>
