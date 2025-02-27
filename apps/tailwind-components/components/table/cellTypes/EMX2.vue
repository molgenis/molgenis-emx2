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
    class="px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <div class="flex flex-row items-center space-x-5">
      <slot class="" name="postfix"></slot>
      <ColumnData
        :data="data"
        :meta-data="metaData"
        class="text-body-base"
      ></ColumnData>
      <slot name="suffix"></slot>
    </div>
  </td>
</template>
