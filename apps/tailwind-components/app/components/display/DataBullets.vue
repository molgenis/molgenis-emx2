<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IRow } from "../../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../../types/display";
import { columnValueToString } from "../../utils/columnValueToString";
import ShowMore from "../ShowMore.vue";

const props = withDefaults(
  defineProps<{
    rows: IRow[];
    resolved: ResolvedDisplay;
    linkTo?: (row: IRow) => string;
    renderLimit?: number;
  }>(),
  {
    // value/List.vue's own precedent for "render everything unless a
    // caller asks for less": effectively unlimited, since DataList no
    // longer pages this layout, so renderLimit is the only "there is
    // more" mechanism it has.
    renderLimit: 1000,
  }
);

const rendered = ref(props.renderLimit);

watch(
  [() => props.renderLimit, () => props.rows],
  () => (rendered.value = props.renderLimit)
);

const displayedRows = computed(() => props.rows.slice(0, rendered.value));
const hasUnrendered = computed(() => props.rows.length > rendered.value);

function renderMore() {
  rendered.value += props.renderLimit;
}

// ShowMore's own control only ever appears while truncate is true; passing
// false to opt out of the line clamp also disables the control regardless
// of hasMore. UNBOUNDED_LINES keeps truncate true, so hasMore alone drives
// the control, while making the clamp itself a no-op: no realistic content
// needs this many lines, so it never independently reports "overflowing"
// and never competes with hasMore for what "more" means.
// ShowMore's own root is an inline span, and its clamp box would be an
// invalid child of <ul> if it wrapped the <li>s, so it sits as a sibling
// below the list instead, with nothing in its own slot.
const UNBOUNDED_LINES = Number.MAX_SAFE_INTEGER;

function titleText(row: IRow): string {
  if (!props.resolved.titleTemplate) {
    return "";
  }
  return columnValueToString(row, props.resolved.titleTemplate) ?? "";
}
</script>

<template>
  <div>
    <ul class="grid gap-1 pl-4 list-disc list-outside">
      <li v-for="(row, rowIndex) in displayedRows" :key="rowIndex">
        <a v-if="linkTo" :href="linkTo(row)" class="text-link underline">
          {{ titleText(row) }}
        </a>
        <span v-else>{{ titleText(row) }}</span>
      </li>
    </ul>
    <ShowMore
      :truncate="true"
      :maxLines="UNBOUNDED_LINES"
      :hasMore="hasUnrendered"
      @showMore="renderMore"
    />
  </div>
</template>
