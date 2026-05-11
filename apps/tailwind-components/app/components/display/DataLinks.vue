<script setup lang="ts">
import { getRowLabel } from "../../utils/displayUtils";
import { useRecordNavigation } from "../../composables/useRecordNavigation";

const props = defineProps<{
  rows: Record<string, any>[];
  rowLabelTemplate?: string;
  schemaId?: string;
  tableId?: string;
}>();

const { navigateToRecord } = useRecordNavigation();

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}
</script>

<template>
  <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
    <li v-for="row in rows" :key="rowKey(row)">
      <a
        v-if="schemaId && tableId"
        href="#"
        class="text-link hover:underline"
        @click.prevent="navigateToRecord(schemaId, tableId, row)"
      >
        {{ getRowLabel(row, rowLabelTemplate) }}
      </a>
      <span v-else>{{ getRowLabel(row, rowLabelTemplate) }}</span>
    </li>
  </ul>
  <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
</template>
