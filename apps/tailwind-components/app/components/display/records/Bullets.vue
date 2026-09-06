<script setup lang="ts">
import type { IRow } from "../../../../../metadata-utils/src/types";
import { columnValueToString } from "../../../utils/columnValueToString";

const props = defineProps<{
  rows: IRow[];
  titleTemplate: string;
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
  <ul class="grid gap-1 pl-4 list-disc list-outside">
    <li v-for="(row, rowIndex) in rows" :key="rowIndex">
      <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
        {{ titleText(row) }}
      </a>
      <span v-else>{{ titleText(row) }}</span>
    </li>
  </ul>
</template>
