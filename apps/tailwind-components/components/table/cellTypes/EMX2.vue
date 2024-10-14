<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
withDefaults(
  defineProps<{
    metaData: IColumn;
    data: any;
    isExpanded: boolean;
  }>(),
  {
    isExpanded: false,
  }
);

const emit = defineEmits(["expand", "contract"]);
</script>
<template>
  <TableCellTypesEmpty v-if="data === null || data === undefined" />
  <TableCellTypesList
    v-else-if="metaData.columnType.endsWith('ARRAY')"
    :metaData="metaData"
    :data="data"
  />
  <TableCellTypesString
    v-else-if="
      metaData.columnType === 'STRING' || metaData.columnType === 'TEXT'
    "
    :metaData="metaData"
    :data="data"
    :isExpanded="isExpanded"
    @expand="() => emit('expand')"
    @contract="() => emit('contract')"
  />
  <TableCellTypesDecimal
    v-else-if="metaData.columnType === 'DECIMAL'"
    :metaData="metaData"
    :data="data"
  />
  <TableCellTypesObject
    v-else-if="metaData.columnType === 'REF'"
    :metaData="metaData"
    :data="data"
  />
  <TableCellTypesObject
    v-else-if="metaData.columnType === 'ONTOLOGY'"
    :metaData="metaData"
    :data="data"
  />
  <td
    v-else
    class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  >
    {{ metaData.columnType }}
  </td>
</template>
