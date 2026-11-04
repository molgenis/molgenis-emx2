<script setup lang="ts">
import { ref } from "vue";
import type {
  columnValue,
  IColumn,
} from "../../../../../metadata-utils/src/types";
import type { UseFilters } from "../../../../types/filters";
import Button from "../../Button.vue";
import Modal from "../../Modal.vue";
import DownloadRows from "./DownloadRows.vue";

const props = withDefaults(
  defineProps<{
    schemaId: string;
    tableId: string;
    filters: UseFilters | null;
    columns?: IColumn[];
    visibleColumns?: IColumn[];
    selectedRowKeys?: Record<string, columnValue>[];
  }>(),
  {
    columns: () => [],
    visibleColumns: () => [],
    selectedRowKeys: () => [],
  }
);

const showDownloadModal = ref(false);
const downloadHref = ref(`/${props.schemaId}/api/csv/${props.tableId}`);
</script>

<template>
  <Button type="outline" icon="download" @click="showDownloadModal = true">
    Download
  </Button>

  <Modal
    id="download-modal"
    v-model:visible="showDownloadModal"
    type="right"
    :background-accessible="false"
    maxWidth="max-w-md"
    title="Download"
  >
    <DownloadRows
      :schemaId="schemaId"
      :tableId="tableId"
      :filters="filters"
      :columns="columns"
      :visibleColumns="visibleColumns"
      :selectedRowKeys="selectedRowKeys"
      v-model:downloadHref="downloadHref"
    />
    <template #footer>
      <div class="flex gap-2 justify-center py-5 h-sidebar-footer items-center">
        <Button
          type="primary"
          icon="download"
          icon-position="right"
          @click="showDownloadModal = false"
          :href="downloadHref"
        >
          Download
        </Button>
      </div>
    </template>
  </Modal>
</template>
