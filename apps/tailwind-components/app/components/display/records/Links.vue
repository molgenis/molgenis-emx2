<script setup lang="ts">
import type { IRow } from "../../../../../metadata-utils/src/types";
import LinkList from "./LinkList.vue";

defineProps<{
  rows: IRow[];
  titleTemplate: string;
  linkTo?: (row: IRow) => string;
  navLabel?: string;
}>();
</script>

<template>
  <LinkList
    :rows="rows"
    :titleTemplate="titleTemplate"
    :linkTo="linkTo"
    :navLabel="navLabel"
    inline
  />
</template>
