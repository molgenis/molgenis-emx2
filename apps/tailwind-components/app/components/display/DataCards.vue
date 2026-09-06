<script setup lang="ts">
import { computed } from "vue";
import {
  isFileValue,
  type IColumn,
  type IRow,
} from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../types/display";
import {
  resolveDisplay,
  resolveTitleAndSubtitle,
} from "../../utils/displayUtils";
import DataPairs from "./DataPairs.vue";
import ValueEMX2 from "../value/EMX2.vue";

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

function title(row: IRow): string {
  return resolveTitleAndSubtitle(row, resolved.value).title;
}

function subtitle(row: IRow): string | undefined {
  return resolveTitleAndSubtitle(row, resolved.value).subtitle;
}

function logoUrl(row: IRow): string | undefined {
  const logoColumn = resolved.value.logoColumn;
  if (!logoColumn) {
    return undefined;
  }
  const value = row[logoColumn.id];
  return isFileValue(value) ? value.url : undefined;
}
</script>

<template>
  <ul class="grid grid-cols-1 lg:grid-cols-2" role="list">
    <!-- Plain `border`, not `border-theme`: that utility also sets
    --border-width-theme, which four themes zero out on purpose, leaving the
    card with no visible edge. No grid gap: cells share a border instead of
    floating apart. -mb-[1px] pulls each row up so stacked cells' horizontal
    borders overlap into one rule, and lg:even:border-l-0 drops the second
    column's left border so the shared vertical rule is single too. -->
    <li
      v-for="(row, rowIndex) in rows"
      :key="rowIndex"
      class="border p-11 relative -mb-[1px] lg:even:border-l-0"
    >
      <img
        v-if="logoUrl(row)"
        :src="logoUrl(row)"
        alt=""
        class="max-h-16 max-w-full mb-2 object-contain"
      />
      <div class="font-bold text-record-heading">
        <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
          {{ title(row) }}
        </a>
        <span v-else>{{ title(row) }}</span>
      </div>
      <span
        v-if="subtitle(row)"
        class="mt-1.5 block md:inline text-record-value"
      >
        {{ subtitle(row) }}
      </span>
      <div v-if="resolved.descriptionColumn" class="mt-1 text-record-value">
        <ValueEMX2
          :metadata="resolved.descriptionColumn"
          :data="row[resolved.descriptionColumn.id]"
          :hide-list-separator="hideListSeparator"
          :max-lines="maxLines"
          :render-limit="renderLimit"
          :truncate="truncate"
        />
      </div>
      <DataPairs
        v-if="resolved.detailColumns.length"
        :columns="resolved.detailColumns"
        :row="row"
        :hide-list-separator="hideListSeparator"
        :max-lines="maxLines"
        :render-limit="renderLimit"
        :truncate="truncate"
      />
    </li>
  </ul>
</template>
