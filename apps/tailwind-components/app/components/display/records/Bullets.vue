<script setup lang="ts">
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import LinkList from "./LinkList.vue";

defineProps<{
  rows: IRow[];
  titleColumn?: IColumn;
  linkTo?: (row: IRow) => string;
  navLabel?: string;
}>();
</script>

<template>
  <LinkList
    :rows="rows"
    :titleColumn="titleColumn"
    :linkTo="linkTo"
    :navLabel="navLabel"
  />
</template>
