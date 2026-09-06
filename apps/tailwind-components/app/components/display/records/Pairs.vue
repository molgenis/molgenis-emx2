<script setup lang="ts">
import { computed } from "vue";
import type { IColumn, IRow } from "../../../../../metadata-utils/src/types";
import ValueEMX2 from "../../value/EMX2.vue";

// `wide` is the one difference between the two layouts that use this
// component: whether the pairs are ever allowed to sit side by side once
// they fit, or always render label-left/value-right. Everything below,
// the per-type cap, hideEmpty and the fold bands, exists only to answer
// "what happens once side-by-side stops fitting", so it has no meaning
// when wide is false and Cards never triggers it.
const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    row: IRow;
    wide?: boolean;
    hideEmpty?: boolean;
  }>(),
  {
    wide: false,
    hideEmpty: true,
  }
);

// Caps a short fixed-format value's width so it does not stretch across the
// full remaining track. Neither TableEMX2 (every column gets one flat
// 240px, `useColumnResize.ts`'s `defaultWidth`) nor any input or value
// component sizes by columnType, so there is no per-type precedent to
// derive bands from. `max-w-xs` (15rem = 240px) is the closest existing
// number in the codebase to that one true precedent, so narrow types reuse
// it rather than a new one; TEXT and everything else stay uncapped.
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

// The owner's rule for the wide shape: once even one pair no longer fits on
// a single row, the whole set folds to key-value (label left, value right,
// dt and dd as direct grid children of a two-column [auto,1fr] grid); once
// every pair fits, labels form one row and values another, so a wrapped
// label cannot push its own value out of line with its neighbours'. That
// column/row split is why dt and dd are direct grid children in the wide
// shape rather than each wrapped in its own div: grid-auto-flow: column
// lets the browser lay every dt in row 1 and every dd in row 2, one column
// per detail column.
// The fold threshold depends on how many detail columns there are, so a
// single fixed variant cannot express it:
//   width(N) = N*160 + (N-1)*56   (the 160px track minimum, the 56px gap)
// width(2)=376px=23.5rem, width(3)=592px=37rem, width(4)=808px=50.5rem,
// width(5)=1024px=64rem. @sm (24rem) is the closest named size to N=2; N=5's
// exact width happens to equal the named @5xl; N=3 and N=4 have no named
// size close enough, so they carry their exact width as an arbitrary value.
// Each string below is written out in full so Tailwind's scanner can see
// it; a computed or concatenated class name is invisible to it.
// resolveDisplay caps its own default at five detail columns, but a caller
// can pass more, so six and above reuse the six-column threshold rather
// than never widening.
const DETAIL_COLUMN_FOLD_STRUCTURE: Record<number, string> = {
  2: "@sm:grid-cols-[repeat(2,minmax(160px,1fr))] @sm:grid-flow-col @sm:grid-rows-2",
  3: "@[37rem]:grid-cols-[repeat(3,minmax(160px,1fr))] @[37rem]:grid-flow-col @[37rem]:grid-rows-2",
  4: "@[50.5rem]:grid-cols-[repeat(4,minmax(160px,1fr))] @[50.5rem]:grid-flow-col @[50.5rem]:grid-rows-2",
  5: "@5xl:grid-cols-[repeat(5,minmax(160px,1fr))] @5xl:grid-flow-col @5xl:grid-rows-2",
  6: "@[77.5rem]:grid-cols-[repeat(6,minmax(160px,1fr))] @[77.5rem]:grid-flow-col @[77.5rem]:grid-rows-2",
};
const MAX_SUPPORTED_FOLD_COLUMNS = 6;

const wideContainerClass = computed(() => {
  const base =
    "@container grid grid-cols-[auto_1fr] items-baseline gap-x-4 gap-y-2";
  const detailColumnCount = props.columns.length;
  if (detailColumnCount < 2) {
    // A single pair can never fail to fit on its own row, so it always
    // shows label left, value right; no wide structure is needed.
    return base;
  }
  const wideStructure =
    DETAIL_COLUMN_FOLD_STRUCTURE[
      Math.min(detailColumnCount, MAX_SUPPORTED_FOLD_COLUMNS)
    ];
  return `${base} ${wideStructure}`;
});

// Which fold band's hide-when-empty rule (see the <style> block) applies to
// this dl, as a plain number for the `data-fold-columns` attribute the
// scoped CSS selects on. undefined omits the attribute, so the CSS matches
// nothing: a single detail column never folds, and hideEmpty lets a caller
// keep every slot in both states.
const detailFoldColumns = computed<number | undefined>(() => {
  if (!props.hideEmpty) {
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
  <!-- A container query is answered by an ANCESTOR container, never by the
  element's own container-type. The bands below sit on the dl, so without this
  wrapper they query nothing and no band ever matches: the pairs stay label-left
  at every width. -->
  <div v-if="wide" class="@container">
    <dl
      class="mt-3"
      :class="wideContainerClass"
      :data-fold-columns="detailFoldColumns"
    >
      <template v-for="column in columns" :key="column.id">
        <dt class="text-record-label font-bold">
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
  <dl v-else class="mt-3 grid gap-1">
    <div v-for="column in columns" :key="column.id" class="flex gap-2">
      <dt class="text-record-label font-bold">
        {{ column.label || column.id }}
      </dt>
      <dd class="text-record-value">
        <ValueEMX2 :metadata="column" :data="row[column.id]" />
      </dd>
    </div>
  </dl>
</template>

<style scoped>
/* Hides an empty detail pair (its dt and its dd), but only while its fold
   band cannot fit every pair on one row (see DETAIL_COLUMN_FOLD_STRUCTURE
   above for the same arithmetic: width(N) = N*160 + (N-1)*56). Above the
   threshold both keep their slot, so the same field still lines up across
   records. dt and dd are separate grid children, not one wrapper, so each
   needs its own rule: dt:has(+ dd:empty) reaches the label from its very
   next sibling being an empty value. Only reachable when wide is true; the
   folded shape below has no data-fold-columns attribute to match.
   The Tailwind container-queries plugin only emits min-width conditions, so
   this max-width form has to be plain CSS, matching ShowMore.vue and other
   components that already use <style scoped> here.
   max-width sits 1px (0.0625rem) below the fold point so the hide window
   and the fold's own min-width structure never overlap at the boundary
   pixel and never leave a pixel-wide gap between them.
     N=2: 376px = 23.5rem  -> 23.4375rem
     N=3: 592px = 37rem    -> 36.9375rem
     N=4: 808px = 50.5rem  -> 50.4375rem
     N=5: 1024px = 64rem   -> 63.9375rem
     N=6: 1240px = 77.5rem -> 77.4375rem (also the catch-all above 6) */
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
