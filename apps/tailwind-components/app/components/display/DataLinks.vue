<script setup lang="ts">
import { NuxtLink } from "#components";
import { getRowLabel } from "../../utils/displayUtils";

const props = defineProps<{
  rows: Record<string, any>[];
  rowLabelTemplate?: string;
  getHref?: (row: Record<string, any>) => string | undefined;
}>();

function rowKey(row: Record<string, any>): string {
  return row.id || row.name || JSON.stringify(row);
}
</script>

<template>
  <ul v-if="rows.length" class="grid gap-1 pl-4 list-disc list-outside">
    <li v-for="row in rows" :key="rowKey(row)">
      <NuxtLink
        v-if="getHref && getHref(row)"
        :to="getHref(row)!"
        class="text-link hover:underline"
      >
        {{ getRowLabel(row, rowLabelTemplate) }}
      </NuxtLink>
      <span v-else>{{ getRowLabel(row, rowLabelTemplate) }}</span>
    </li>
  </ul>
  <p v-else class="text-gray-400 dark:text-gray-500 italic">No items</p>
</template>
