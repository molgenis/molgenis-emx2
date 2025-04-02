<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";

const props = defineProps<{
  metaData: IColumn;
  data: string[] | number[] | Record<string, any>;
}>();

const elementType = computed(() => props.metaData.columnType.split("_")[0]);
</script>

<template>
  <template v-for="listElement in data">
    <TableCellTypesString
      v-if="elementType === 'STRING'"
      :metaData="metaData"
      :data="listElement as string"
    />
    <TableCellTypesDecimal
      v-else-if="elementType === 'DECIMAL'"
      :metaData="metaData"
      :data="listElement as number"
    />
    <TableCellTypesLong
      v-else-if="elementType === 'LONG'"
      :metaData="metaData"
      :data="listElement as number"
    />
    <TableCellTypesBool
      v-else-if="elementType === 'BOOL'"
      :metaData="metaData"
      :data="listElement as boolean"
    />
    <TableCellTypesObject
      v-else-if="elementType === 'REF'"
      :metaData="metaData"
      :data="listElement"
    />
    <TableCellTypesObject
      v-else-if="elementType === 'ONTOLOGY'"
      :metaData="metaData"
      :data="listElement"
    />
    <span v-else>{{ elementType }}</span>
  </template>
</template>
