<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: any;
}>();
</script>
<template>
  <td
    v-if="data === null || data === undefined"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  ></td>

  <td
    v-else-if="metaData.columnType.endsWith('ARRAY')"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <TableCellTypesList :metaData="metaData" :data="data" />
  </td>

  <td
    v-else-if="metaData.columnType === 'STRING'"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <TableCellTypesString :metaData="metaData" :data="data" />
  </td>

  <td
    v-else-if="metaData.columnType === 'DECIMAL'"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <TableCellTypesDecimal :metaData="metaData" :data="data" />
  </td>

  <td
    v-else-if="metaData.columnType === 'REF'"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <TableCellTypesObject :metaData="metaData" :data="data" />
  </td>

  <td
    v-else-if="metaData.columnType === 'ONTOLOGY'"
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 h-4 overflow-ellipsis whitespace-nowrap overflow-hidden"
  >
    <TableCellTypesObject :metaData="metaData" :data="data" />
  </td>

  <td
    v-else
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  >
    {{ metaData.columnType }}
  </td>
</template>
