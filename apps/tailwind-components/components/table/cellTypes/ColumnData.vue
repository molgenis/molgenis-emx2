<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
defineProps<{
  metaData: IColumn;
  data: any;
}>();
</script>
<template>
  <template v-if="data === null || data === undefined"></template>
  <TableCellTypesList
    v-else-if="metaData.columnType.endsWith('ARRAY')"
    :metaData="metaData"
    :data="data"
  />

  <TableCellTypesString
    v-else-if="metaData.columnType === 'STRING'"
    :metaData="metaData"
    :data="data"
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

  <template v-else> {{ metaData.columnType }} </template>
</template>
