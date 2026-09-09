<script setup lang="ts">
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import { columnValueToString } from "../../../utils/columnValueToString";
import ValueEMX2 from "../../value/EMX2.vue";
import Table from "../../Table.vue";
import TableRow from "../../TableRow.vue";
import TableHeadRow from "../../TableHeadRow.vue";
import TableCell from "../../TableCell.vue";
import TableHeadCell from "../../table/TableHeadCell.vue";

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
    <Table>
      <template #head>
        <TableHeadRow stacked>
          <TableHeadCell>Title</TableHeadCell>
          <TableHeadCell v-for="column in columns" :key="column.id">
            {{ column.label || column.id }}
          </TableHeadCell>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow v-for="(row, rowIndex) in rows" :key="rowIndex" stacked>
          <TableCell stacked label="Title">
            <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
              {{ titleText(row) }}
            </a>
            <span v-else>{{ titleText(row) }}</span>
          </TableCell>
          <TableCell
            v-for="column in columns"
            :key="column.id"
            stacked
            :label="column.label || column.id"
          >
            <ValueEMX2 :metadata="column" :data="row[column.id]" />
          </TableCell>
        </TableRow>
      </template>
    </Table>
  </div>
</template>
