<script setup lang="ts">
import { useRouter } from "#app/composables/router";
import { ref } from "vue";
import type { IRow } from "../../../metadata-utils/src/types";
import BreadCrumbs from "../../../tailwind-components/app/components/BreadCrumbs.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import CellDetailModal from "../../../tailwind-components/app/components/table/cellDetail/CellDetailModal.vue";
import DetailView from "../../../tailwind-components/app/components/display/DetailView.vue";
import RecordActions from "../../../tailwind-components/app/components/display/RecordActions.vue";
import type {
  cellPayload,
  Crumb,
} from "../../../tailwind-components/types/types";

const props = defineProps<{
  schemaId: string;
  tableId: string;
  rowId: IRow;
  title: string;
  crumbs: Crumb[];
}>();

const emit = defineEmits<{
  (event: "rowChanged"): void;
}>();

const router = useRouter();

const showModal = ref(false);
const cellDetailPayload = ref<cellPayload>();
const recordViewKey = ref(0);

function afterRecordChanged() {
  recordViewKey.value++;
  emit("rowChanged");
}

function afterRowDeleted() {
  router.push(`/${props.schemaId}/${props.tableId}`);
}

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

      <RecordActions
        :schema-id="schemaId"
        :table-id="tableId"
        :row-id="rowId"
        @changed="afterRecordChanged"
        @deleted="afterRowDeleted"
      />
    </template>
  </DetailView>

  <CellDetailModal
    v-if="cellDetailPayload"
    :payload="cellDetailPayload"
    :schemaId="schemaId"
    v-model:showModal="showModal"
    @update:cellDetailPayload="cellDetailPayload = $event"
  />
</template>
