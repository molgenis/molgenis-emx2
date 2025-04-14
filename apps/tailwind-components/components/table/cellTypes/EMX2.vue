<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import ColumnData from "./ColumnData.vue";
defineProps<{
  metaData: IColumn;
  data: any;
}>();
</script>
<template>
  <td
    class="h-4 py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <div class="flex justify-between">
      <span
        class="text-body-base flex items-center shrink overflow-ellipsis whitespace-nowrap overflow-hidden flex-0"
      >
        <ColumnData :data="data" :meta-data="metaData" />
      </span>
      <slot />
    </div>
  </td>
</template>
