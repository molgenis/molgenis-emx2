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
  return columnValueToString(row, props.resolved.titleTemplate) ?? "";
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
