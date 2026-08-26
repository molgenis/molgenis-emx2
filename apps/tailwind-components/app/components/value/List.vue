<script setup lang="ts">
import { computed, ref } from "vue";
import { isRefbackType } from "../../../../metadata-utils/src";
import type {
  columnValue,
  IColumn,
} from "../../../../metadata-utils/src/types";
import type { ListPayload } from "../../../types/types";
import {
  assertBooleanValue,
  assertNumberValue,
  assertRowValue,
  assertStringValue,
  assertTableValue,
  toRefColumn,
} from "../../utils/typeUtils";
import ValueBool from "./Bool.vue";
import ValueDate from "./Date.vue";
import ValueDateTime from "./DateTime.vue";
import ValueDecimal from "./Decimal.vue";
import ValueEmail from "./Email.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueInt from "./Int.vue";
import ValueLong from "./Long.vue";
import ValueObject from "./Object.vue";
import ValueString from "./String.vue";
import ValueRefBack from "./RefBack.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data?: columnValue[] | null;
    hideListSeparator?: boolean;
    /** Collapse the list when it holds more values than this. */
    maxItems?: number;
    /** How many lines a collapsed list occupies. The visible bound is space, not count. */
    maxLines?: number;
    /** How many values reach the DOM at all. */
    renderLimit?: number;
  }>(),
  {
    hideListSeparator: false,
    maxLines: 3,
    renderLimit: 100,
  }
);

const elementType = computed(
  () => props.metadata.columnType.split("_ARRAY")[0]
);

const collapsed = ref(true);

/**
 * Every rendered value stays in the DOM, collapsed or not, so a crawler, in-page
 * search and a screen reader all still reach it. `renderLimit` only stops a huge
 * refback from painting without bound; past it, the bound belongs in the query.
 */
const displayedData = computed(() => {
  if (!props.data) return props.data;
  return props.data.slice(0, props.renderLimit);
});

const isCollapsible = computed(
  () => !!props.maxItems && !!props.data && props.data.length > props.maxItems
);

/**
 * Counts what expanding actually reveals, which is bounded by `renderLimit`, not
 * by how many values the record holds. Promising more than is in the DOM would
 * make the control lie on a long refback.
 */
const hiddenCount = computed(() => {
  if (!isCollapsible.value || !props.data || !props.maxItems) return 0;
  const rendered = Math.min(props.data.length, props.renderLimit);
  return Math.max(0, rendered - props.maxItems);
});

/**
 * Clamping in CSS rather than slicing the array is what keeps the values in the
 * DOM. It also costs no measurement: `maxItems` decides whether a control appears,
 * which the server can work out, and CSS decides how much shows.
 */
const isClamped = computed(() => collapsed.value && isCollapsible.value);

const clampStyle = computed(() =>
  isClamped.value ? { "--value-list-lines": String(props.maxLines) } : undefined
);

const emit = defineEmits<{
  (e: "listRefCellClicked", data: ListPayload): void;
}>();

function handleCellClick() {
  if (!props.data) {
    return;
  }
  emit("listRefCellClicked", { metadata: props.metadata, data: props.data });
}
</script>

<template>
  <span>
    <span :class="{ 'value-list-clamp': isClamped }" :style="clampStyle">
      <template v-for="(listElement, index) in displayedData">
        <ValueString
          v-if="
            elementType === 'STRING' ||
            elementType === 'AUTO_ID' ||
            elementType === 'PERIOD' ||
            elementType === 'UUID'
          "
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <ValueString
          v-else-if="elementType === 'TEXT'"
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <ValueDecimal
          v-else-if="elementType === 'DECIMAL'"
          :metadata="metadata"
          :data="assertNumberValue(listElement)"
        />
        <ValueLong
          v-else-if="elementType === 'LONG'"
          :metadata="metadata"
          :data="assertNumberValue(listElement)"
        />
        <ValueInt
          v-else-if="
            elementType === 'INT' || elementType === 'NON_NEGATIVE_INT'
          "
          :metadata="metadata"
          :data="assertNumberValue(listElement)"
        />
        <ValueBool
          v-else-if="elementType === 'BOOL'"
          :metadata="metadata"
          :data="assertBooleanValue(listElement)"
        />
        <ValueEmail
          v-else-if="elementType === 'EMAIL'"
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <ValueHyperlink
          v-else-if="elementType === 'HYPERLINK'"
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <ValueObject
          v-else-if="
            elementType === 'REF' ||
            elementType === 'MULTISELECT' ||
            elementType === 'CHECKBOX'
          "
          :metadata="metadata"
          :data="assertRowValue(listElement)"
          @refCellClicked="handleCellClick"
        />
        <ValueRefBack
          v-else-if="isRefbackType(metadata.columnType)"
          :metadata="toRefColumn(metadata)"
          :data="assertTableValue(listElement)"
          @refBackCellClicked="handleCellClick"
        />
        <ValueObject
          v-else-if="elementType === 'ONTOLOGY'"
          :metadata="metadata"
          :data="assertRowValue(listElement)"
          @refCellClicked="handleCellClick"
        />
        <ValueDate
          v-else-if="elementType === 'DATE'"
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <ValueDateTime
          v-else-if="elementType === 'DATETIME'"
          :metadata="metadata"
          :data="assertStringValue(listElement)"
        />
        <span v-else>{{ elementType }}</span>
        <span
          v-if="
            Number(displayedData?.length) - 1 !== Number(index) &&
            !hideListSeparator
          "
        >
          ,&nbsp;
        </span>
      </template>
    </span>
    <button
      v-if="isCollapsible && collapsed"
      class="text-link text-body-sm ml-1"
      :aria-expanded="false"
      @click="collapsed = false"
    >
      +{{ hiddenCount }} more
    </button>
    <button
      v-if="isCollapsible && !collapsed"
      class="text-link text-body-sm ml-1"
      title="Show less"
      aria-label="Show less"
      :aria-expanded="true"
      @click="collapsed = true"
    >
      less
    </button>
  </span>
</template>

<style scoped>
/* Bound the collapsed list by space, not by item count. The values stay in the DOM. */
.value-list-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--value-list-lines);
  line-clamp: var(--value-list-lines);
  overflow: hidden;
}
</style>
