<script setup lang="ts">
import { isFileValue, type IRow } from "../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";
import DataPairs from "./DataPairs.vue";
import ShowMore from "../ShowMore.vue";

const props = withDefaults(
  defineProps<{
    rows: IRow[];
    resolved: ResolvedDisplay;
    linkTo?: (row: IRow) => string;
    hideListSeparator?: boolean;
    maxLines?: number;
    renderLimit?: number;
    truncate?: boolean;
    hideEmpty?: boolean;
  }>(),
  {
    // Vue casts an unset boolean prop to false unless a default is given.
    // hideEmpty is DataPairs' default (true) to make, not DataRows', so
    // this forwards a genuine undefined rather than silently overriding it.
    hideEmpty: undefined,
  }
);

function titleText(row: IRow): string {
  if (!props.resolved.titleTemplate) {
    return "";
  }
  return columnValueToString(row, props.resolved.titleTemplate) ?? "";
}

function descriptionText(row: IRow): string | undefined {
  if (!props.resolved.descriptionTemplate) {
    return undefined;
  }
  return (
    columnValueToString(row, props.resolved.descriptionTemplate) || undefined
  );
}

function logoUrl(row: IRow): string | undefined {
  const logoColumn = props.resolved.logoColumn;
  if (!logoColumn) {
    return undefined;
  }
  const value = row[logoColumn.id];
  return isFileValue(value) ? value.url : undefined;
}
</script>

<template>
  <ul class="grid grid-cols-1" role="list">
    <!-- Plain `border`, not `border-theme`: that utility also sets
    --border-width-theme, which four themes zero out on purpose, leaving the
    row with no visible edge. No grid gap: rows share a border instead of
    floating apart. -mb-[1px] pulls each row up so stacked rows' horizontal
    borders overlap into one rule. -->
    <li
      v-for="(row, rowIndex) in rows"
      :key="rowIndex"
      class="border p-11 relative -mb-[1px]"
    >
      <img
        v-if="logoUrl(row)"
        :src="logoUrl(row)"
        alt=""
        class="max-h-16 max-w-full mb-2 object-contain"
      />
      <div class="font-bold text-record-heading">
        <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
          {{ titleText(row) }}
        </a>
        <span v-else>{{ titleText(row) }}</span>
      </div>
      <ShowMore
        v-if="descriptionText(row)"
        class="mt-1 text-record-value"
        :max-lines="maxLines"
        :truncate="truncate"
      >
        {{ descriptionText(row) }}
      </ShowMore>
      <DataPairs
        v-if="resolved.detailColumns.length"
        wide
        :columns="resolved.detailColumns"
        :row="row"
        :hide-empty="hideEmpty"
        :hide-list-separator="hideListSeparator"
        :max-lines="maxLines"
        :render-limit="renderLimit"
        :truncate="truncate"
      />
    </li>
  </ul>
</template>
