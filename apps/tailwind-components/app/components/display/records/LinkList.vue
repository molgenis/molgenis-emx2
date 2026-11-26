<script setup lang="ts">
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import ValueEMX2 from "../../value/EMX2.vue";

withDefaults(
  defineProps<{
    rows: IRow[];
    titleColumn?: IColumn;
    linkTo?: (row: IRow) => string;
    navLabel?: string;
    inline?: boolean;
  }>(),
  {
    navLabel: "Records",
    inline: false,
  }
);
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
          <ValueEMX2
            v-if="titleColumn"
            :metadata="titleColumn"
            :data="row[titleColumn.id]"
          />
        </NuxtLink>
        <ValueEMX2
          v-else-if="titleColumn"
          :metadata="titleColumn"
          :data="row[titleColumn.id]"
        />
        <span v-if="inline && rowIndex < rows.length - 1">, </span>
      </li>
    </ul>
  </component>
</template>
