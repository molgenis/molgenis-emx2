<script setup lang="ts">
import type { IRow } from "../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";

const props = defineProps<{
  rows: IRow[];
  resolved: ResolvedDisplay;
  linkTo?: (row: IRow) => string;
}>();

function titleText(row: IRow): string {
  if (!props.resolved.titleTemplate) {
    return "";
  }
  return columnValueToString(row, props.resolved.titleTemplate) ?? "";
}
</script>

<template>
  <p>
    <template v-for="(row, rowIndex) in rows" :key="rowIndex">
      <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">{{
        titleText(row)
      }}</a>
      <span v-else>{{ titleText(row) }}</span>
      <span v-if="rowIndex < rows.length - 1">, </span>
    </template>
  </p>
</template>
