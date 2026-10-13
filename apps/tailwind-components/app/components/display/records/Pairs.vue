<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import ValueEMX2 from "../../value/EMX2.vue";

const props = defineProps<{
  columns: IColumn[];
  row: IRow;
  wide?: boolean;
  showEmpty?: boolean;
}>();

const NARROW_DETAIL_TYPES = new Set([
  "BOOL",
  "INT",
  "NON_NEGATIVE_INT",
  "LONG",
  "DECIMAL",
  "DATE",
  "UUID",
]);

function isNarrowType(column: IColumn): boolean {
  return NARROW_DETAIL_TYPES.has(column.columnType);
}

// Band N starts at N*160 + (N-1)*56 px. Class names stay literal so Tailwind's scanner sees them.
const DETAIL_COLUMN_FOLD_STRUCTURE: Record<number, string> = {
  2: "@sm:grid-cols-[repeat(2,minmax(160px,1fr))] @sm:grid-flow-col @sm:grid-rows-2",
  3: "@[37rem]:grid-cols-[repeat(3,minmax(160px,1fr))] @[37rem]:grid-flow-col @[37rem]:grid-rows-2",
  4: "@[50.5rem]:grid-cols-[repeat(4,minmax(160px,1fr))] @[50.5rem]:grid-flow-col @[50.5rem]:grid-rows-2",
  5: "@5xl:grid-cols-[repeat(5,minmax(160px,1fr))] @5xl:grid-flow-col @5xl:grid-rows-2",
  6: "@[77.5rem]:grid-cols-[repeat(6,minmax(160px,1fr))] @[77.5rem]:grid-flow-col @[77.5rem]:grid-rows-2",
};
const MAX_SUPPORTED_FOLD_COLUMNS = 6;

const wideFoldStructureClass = computed(() => {
  const detailColumnCount = props.columns.length;
  if (detailColumnCount < 2) {
    return "";
  }
  return DETAIL_COLUMN_FOLD_STRUCTURE[
    Math.min(detailColumnCount, MAX_SUPPORTED_FOLD_COLUMNS)
  ];
});

const detailFoldColumns = computed<number | undefined>(() => {
  if (props.showEmpty) {
    return undefined;
  }
  const detailColumnCount = props.columns.length;
  if (detailColumnCount < 2) {
    return undefined;
  }
  return Math.min(detailColumnCount, MAX_SUPPORTED_FOLD_COLUMNS);
});
</script>

<template>
  <div v-if="wide" class="@container">
    <dl
      class="mt-3 @container grid grid-cols-[auto_1fr] items-baseline gap-x-4 gap-y-2"
      :class="wideFoldStructureClass"
      :data-fold-columns="detailFoldColumns"
    >
      <template v-for="column in columns" :key="column.id">
        <dt class="text-title-contrast font-normal opacity-70">
          {{ column.label || column.id }}
        </dt>
        <dd
          class="text-record-value"
          :class="{ 'max-w-xs': isNarrowType(column) }"
        >
          <ValueEMX2 :metadata="column" :data="row[column.id]" />
        </dd>
      </template>
    </dl>
  </div>
  <dl v-else class="mt-3 grid grid-cols-3 gap-x-4 gap-y-1">
    <template v-for="column in columns" :key="column.id">
      <dt class="text-title-contrast font-normal opacity-70">
        {{ column.label || column.id }}
      </dt>
      <dd class="col-span-2 text-record-value">
        <ValueEMX2 :metadata="column" :data="row[column.id]" />
      </dd>
    </template>
  </dl>
</template>

<style scoped>
/* Each max-width sits 1px below the band start in DETAIL_COLUMN_FOLD_STRUCTURE. */
@container (max-width: 23.4375rem) {
  [data-fold-columns="2"] dt:has(+ dd:empty),
  [data-fold-columns="2"] dd:empty {
    display: none;
  }
}
@container (max-width: 36.9375rem) {
  [data-fold-columns="3"] dt:has(+ dd:empty),
  [data-fold-columns="3"] dd:empty {
    display: none;
  }
}
@container (max-width: 50.4375rem) {
  [data-fold-columns="4"] dt:has(+ dd:empty),
  [data-fold-columns="4"] dd:empty {
    display: none;
  }
}
@container (max-width: 63.9375rem) {
  [data-fold-columns="5"] dt:has(+ dd:empty),
  [data-fold-columns="5"] dd:empty {
    display: none;
  }
}
@container (max-width: 77.4375rem) {
  [data-fold-columns="6"] dt:has(+ dd:empty),
  [data-fold-columns="6"] dd:empty {
    display: none;
  }
}
</style>
