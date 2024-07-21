<script setup lang="ts">
import type { IColumn } from "../../meta-data-utils/src/types";

defineProps<{
  columns: IColumn[];
  rows: Record<string, any>[];
}>();
</script>
<template>
  <div class="overflow-x-auto overscroll-contain">
    <table class="text-left bg-white table-fixed w-full">
      <thead>
        <tr class="">
          <th
            v-for="column in columns"
            class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left w-64"
          >
            <div class="overflow-ellipsis whitespace-nowrap overflow-hidden">
              {{ column.label }}
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows">
          <TableCellTypesEMX2
            v-for="column in columns"
            :metaData="column"
            :data="row[column.id]"
          />
        </tr>
      </tbody>
    </table>
  </div>
</template>
