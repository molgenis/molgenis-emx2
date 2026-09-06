<script setup lang="ts">
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import { columnValueToString } from "../../../utils/columnValueToString";
import ValueEMX2 from "../../value/EMX2.vue";

const props = defineProps<{
  rows: IRow[];
  titleTemplate: string;
  columns: IColumn[];
  linkTo?: (row: IRow) => string;
}>();

function titleText(row: IRow): string {
  if (!props.titleTemplate) {
    return "";
  }
  return columnValueToString(row, props.titleTemplate) ?? "";
}
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full table-auto">
      <thead>
        <tr>
          <th
            scope="col"
            class="py-2.5 px-2.5 text-left text-table-column-header"
          >
            Title
          </th>
          <th
            v-for="column in columns"
            :key="column.id"
            scope="col"
            class="py-2.5 px-2.5 text-left text-table-column-header"
          >
            {{ column.label || column.id }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, rowIndex) in rows"
          :key="rowIndex"
          class="border-b border-theme"
        >
          <td class="py-2.5 px-2.5 font-bold text-table-row">
            <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
              {{ titleText(row) }}
            </a>
            <span v-else>{{ titleText(row) }}</span>
          </td>
          <td
            v-for="column in columns"
            :key="column.id"
            class="py-2.5 px-2.5 text-table-row"
          >
            <ValueEMX2 :metadata="column" :data="row[column.id]" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
