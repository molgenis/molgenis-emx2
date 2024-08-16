<script setup lang="ts">
import type { IColumn } from "../../../../meta-data-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: any;
}>();
</script>
<template>
  <td
    v-if="typeof data === 'string'"
    class="py-2.5 px-2.5 border-b border-gray-200 first:font-bold first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    {{ data }}
  </td>
  <td
    v-else-if="data === null || data === undefined"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  ></td>
  <td
    v-else
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  >
    {{ typeof data }}
  </td>
</template>
