<script setup lang="ts">
import type {
  IColumn,
  IRow,
  IDisplayConfig,
} from "../../../../metadata-utils/src/types";
import CardList from "../CardList.vue";
import CardListItem from "../CardListItem.vue";
import RecordCard from "./RecordCard.vue";

defineProps<{
  columns: IColumn[];
  rows: IRow[];
  columnConfig?: Record<string, IDisplayConfig>;
}>();

defineSlots<{
  actions?: (props: { row: IRow }) => any;
  default?: (props: { row: IRow; columns: IColumn[] }) => any;
}>();
</script>

<template>
  <CardList>
    <CardListItem v-for="(row, rowIndex) in rows" :key="rowIndex">
      <RecordCard :row="row" :columns="columns" :column-config="columnConfig">
        <template v-if="$slots.actions" #actions="{ row: r }">
          <slot name="actions" :row="r" />
        </template>
        <template v-if="$slots.default" #default="{ row: r, columns: cols }">
          <slot :row="r" :columns="cols" />
        </template>
      </RecordCard>
    </CardListItem>
  </CardList>
</template>
