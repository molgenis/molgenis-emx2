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
    :title-template="titleTemplate"
    :link-to="linkTo"
    :nav-label="navLabel"
  />
</template>
