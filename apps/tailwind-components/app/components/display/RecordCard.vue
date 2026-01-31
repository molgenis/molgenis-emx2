<script setup lang="ts">
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";

defineProps<{
  row: IRow;
  columns: IColumn[];
}>();
</script>

<template>
  <div class="border border-black/10 rounded-lg p-3 bg-white">
    <div
      v-for="col in columns"
      :key="col.id"
      class="flex justify-between gap-2 py-1 border-b border-black/5 last:border-0"
    >
      <span class="text-body-sm font-semibold text-table-column-header shrink-0">
        {{ col.label || col.id }}
      </span>
      <span class="text-body-sm text-table-row text-right">
        <ValueEMX2 :metadata="col" :data="row[col.id]" />
      </span>
    </div>
  </div>
</template>
