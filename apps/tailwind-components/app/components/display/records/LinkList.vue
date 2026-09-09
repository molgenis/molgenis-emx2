<script setup lang="ts">
import type { IRow } from "../../../../../metadata-utils/src/types";
import { columnValueToString } from "../../../utils/columnValueToString";

const props = withDefaults(
  defineProps<{
    rows: IRow[];
    titleTemplate: string;
    linkTo?: (row: IRow) => string;
    navLabel?: string;
    // Links renders titles as a comma-separated run on one line; Bullets
    // stacks them one per line. Same list, only the layout option differs,
    // the way ChartLegend takes stackLegend rather than being two components.
    inline?: boolean;
  }>(),
  {
    navLabel: "Records",
    inline: false,
  }
);

function titleText(row: IRow): string {
  if (!props.titleTemplate) {
    return "";
  }
  return columnValueToString(row, props.titleTemplate) ?? "";
}
</script>

<template>
  <component
    :is="linkTo ? 'nav' : 'div'"
    :aria-label="linkTo ? navLabel : undefined"
  >
    <ul
      :class="inline ? 'list-none' : 'grid gap-1 pl-4 list-disc list-outside'"
    >
      <li
        v-for="(row, rowIndex) in rows"
        :key="rowIndex"
        :class="inline ? 'inline' : undefined"
      >
        <NuxtLink v-if="linkTo" :to="linkTo(row)" class="text-link underline">
          {{ titleText(row) }}
        </NuxtLink>
        <span v-else>{{ titleText(row) }}</span>
        <span v-if="inline && rowIndex < rows.length - 1">, </span>
      </li>
    </ul>
  </component>
</template>
