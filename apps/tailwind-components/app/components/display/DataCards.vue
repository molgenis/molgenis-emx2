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
  }>(),
  {
    columnCount: 2,
  }
);

const gridClass = computed(() =>
  props.columnCount === 2 ? "grid-cols-2" : "grid-cols-1"
);

function titleText(row: IRow): string {
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
  <ul class="grid gap-4" role="list" :class="gridClass">
    <li
      v-for="(row, rowIndex) in rows"
      :key="rowIndex"
      class="border border-theme rounded-base p-4"
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
      <dl v-if="resolved.detailColumns.length" class="mt-3 grid gap-1">
        <div
          v-for="column in resolved.detailColumns"
          :key="column.id"
          class="flex gap-2"
        >
          <dt class="text-record-label font-bold">
            {{ column.label || column.id }}
          </dt>
          <dd class="text-record-value">
            <ValueEMX2 :metadata="column" :data="row[column.id]" />
          </dd>
        </div>
      </dl>
    </li>
  </ul>
</template>
