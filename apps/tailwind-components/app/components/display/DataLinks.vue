<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";
import { resolveDisplay } from "../../utils/displayUtils";

// hideListSeparator/maxLines/renderLimit/truncate are unused here (DataLinks
// renders the title only, no ValueEMX2), but every layout takes the same
// config surface so DataList can forward them without a per-layout branch.
const props = defineProps<{
  rows: IRow[];
  columns?: IColumn[];
  displayConfig?: DisplayConfig;
  linkTo?: (row: IRow) => string;
  hideListSeparator?: boolean;
  maxLines?: number;
  renderLimit?: number;
  truncate?: boolean;
}>();

const resolved = computed(() =>
  resolveDisplay(props.columns ?? [], props.displayConfig)
);

function titleText(row: IRow): string {
  if (!resolved.value.titleTemplate) {
    return "";
  }
  return columnValueToString(row, resolved.value.titleTemplate) ?? "";
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
