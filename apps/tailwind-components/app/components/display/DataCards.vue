<script setup lang="ts">
import { computed } from "vue";
import { isFileValue, type IRow } from "../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";
import ValueEMX2 from "../value/EMX2.vue";

const props = withDefaults(
  defineProps<{
    rows: IRow[];
    resolved: ResolvedDisplay;
    linkTo?: (row: IRow) => string;
    columnCount?: 1 | 2;
    hideListSeparator?: boolean;
    maxLines?: number;
    renderLimit?: number;
    truncate?: boolean;
  }>(),
  {
    columnCount: 2,
  }
);

const gridClass = computed(() =>
  props.columnCount === 2 ? "grid-cols-1 lg:grid-cols-2" : "grid-cols-1"
);

// No gap: cells share a border instead of floating apart. -mb-[1px] pulls
// each row up so stacked cells' horizontal borders overlap into one rule.
// At the two-column density, the right-hand column drops its left border so
// the shared vertical rule is single too.
const itemClass = computed(() =>
  props.columnCount === 2 ? "lg:even:border-l-0" : ""
);

// LIST density (columnCount 1) only: detail pairs sit label-above-value,
// side by side while they fit, and wrap onto their own line when they do
// not. The wrapping is driven by each pair's own min-width, not a viewport
// breakpoint, so it degrades on its own regardless of how many detail
// columns are configured. 160px matches the min-width the catalogue's own
// ResourceCard.vue already gives its title column, the closest existing
// precedent for a compact field-scale text block that needs room before it
// wraps. CARDS density keeps the stacked label-beside-value rows.
const detailsContainerClass = computed(() =>
  props.columnCount === 1
    ? "mt-3 flex flex-wrap gap-x-14 gap-y-2"
    : "mt-3 grid gap-1"
);
const detailsPairClass = computed(() =>
  props.columnCount === 1 ? "flex flex-col min-w-[160px]" : "flex gap-2"
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
  <ul class="grid" role="list" :class="gridClass">
    <!-- Plain `border`, not `border-theme`: that utility also sets
    --border-width-theme, which four themes zero out on purpose, leaving the
    card with no visible edge. -->
    <li
      v-for="(row, rowIndex) in rows"
      :key="rowIndex"
      class="border p-11 relative -mb-[1px]"
      :class="itemClass"
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
      <p v-if="descriptionText(row)" class="mt-1 text-record-value">
        {{ descriptionText(row) }}
      </p>
      <dl v-if="resolved.detailColumns.length" :class="detailsContainerClass">
        <div
          v-for="column in resolved.detailColumns"
          :key="column.id"
          :class="detailsPairClass"
        >
          <dt class="text-record-label font-bold">
            {{ column.label || column.id }}
          </dt>
          <dd class="text-record-value">
            <ValueEMX2
              :metadata="column"
              :data="row[column.id]"
              :hide-list-separator="hideListSeparator"
              :max-lines="maxLines"
              :render-limit="renderLimit"
              :truncate="truncate"
            />
          </dd>
        </div>
      </dl>
    </li>
  </ul>
</template>
