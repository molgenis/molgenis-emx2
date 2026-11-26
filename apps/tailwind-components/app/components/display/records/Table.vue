<script setup lang="ts">
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import { computed } from "vue";
import ValueEMX2 from "../../value/EMX2.vue";
import Table from "../../Table.vue";
import TableRow from "../../TableRow.vue";
import TableHeadRow from "../../TableHeadRow.vue";
import TableCell from "../../TableCell.vue";
import TableHead from "../../TableHead.vue";

const props = defineProps<{
  rows: IRow[];
  titleColumn?: IColumn;
  columns: IColumn[];
  linkTo?: (row: IRow) => string;
}>();

const otherColumns = computed(() =>
  props.columns.filter((column) => column.id !== props.titleColumn?.id)
);
</script>

<template>
  <div class="overflow-x-auto">
    <Table>
      <template #head>
        <TableHeadRow stacked>
          <TableHead v-if="titleColumn">
            {{ titleColumn.label || titleColumn.id }}
          </TableHead>
          <TableHead v-for="column in otherColumns" :key="column.id">
            {{ column.label || column.id }}
          </TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow v-for="(row, rowIndex) in rows" :key="rowIndex" stacked>
          <TableCell
            v-if="titleColumn"
            stacked
            :label="titleColumn.label || titleColumn.id"
          >
            <NuxtLink
              v-if="linkTo"
              :to="linkTo(row)"
              class="text-link underline"
            >
              <ValueEMX2 :metadata="titleColumn" :data="row[titleColumn.id]" />
            </NuxtLink>
            <ValueEMX2
              v-else
              :metadata="titleColumn"
              :data="row[titleColumn.id]"
            />
          </TableCell>
          <TableCell
            v-for="column in otherColumns"
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
