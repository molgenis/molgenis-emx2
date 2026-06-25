<template>
  <td class="group/cell p-2 border-b min-h-8">
    <slot name="row-actions"></slot>
    <div class="relative flex overflow-hidden items-center">
      <div v-tooltip.bottom="tooltipContent" class="relative flex-1 min-w-0">
        <div class="truncate" :class="{ invisible: isEllipsisActive }" ref="cellRef">
        <slot>
          <template v-if="metadata && data !== undefined && data !== null">
            <ValueList
              v-if="
                metadata.columnType.endsWith('ARRAY') ||
                metadata.columnType === 'MULTISELECT' ||
                metadata.columnType === 'CHECKBOX'
              "
              :metadata="metadata"
              :data="assertListValue(data)"
              @listRefCellClicked="$emit('cellClicked', $event)"
            />

            <ValueString
              v-else-if="
                metadata.columnType === 'STRING' ||
                metadata.columnType === 'DATE' ||
                metadata.columnType === 'DATETIME' ||
                metadata.columnType === 'AUTO_ID' ||
                metadata.columnType === 'UUID' ||
                metadata.columnType === 'PERIOD'
              "
              :metadata="metadata"
              :data="assertStringValue(data)"
            />

            <ValueText
              v-else-if="metadata.columnType === 'TEXT'"
              :metadata="metadata"
              :data="assertStringValue(data)"
            />

            <ValueDecimal
              v-else-if="metadata.columnType === 'DECIMAL'"
              :metadata="metadata"
              :data="assertNumberValue(data)"
            />

            <ValueLong
              v-else-if="metadata.columnType === 'LONG'"
              :metadata="metadata"
              :data="typeof data === 'number' ? data : Number(data)"
            />

            <ValueInt
              v-else-if="
                metadata.columnType === 'INT' ||
                metadata.columnType === 'NON_NEGATIVE_INT'
              "
              :metadata="metadata"
              :data="typeof data === 'number' ? data : Number(data)"
            />

            <ValueRef
              v-else-if="
                metadata.columnType === 'REF' ||
                metadata.columnType === 'RADIO' ||
                metadata.columnType === 'SELECT'
              "
              :metadata="toRefColumn(metadata)"
              :data="assertRowValue(data)"
              @refCellClicked="$emit('cellClicked', $event)"
            />

            <ValueObject
              v-else-if="metadata.columnType === 'ONTOLOGY'"
              :metadata="metadata"
              :data="assertRowValue(data)"
              @refCellClicked="$emit('cellClicked', $event)"
            />

            <ValueBool
              v-else-if="metadata.columnType === 'BOOL'"
              :metadata="metadata"
              :data="assertBooleanValue(data)"
            />

            <ValueEmail
              v-else-if="metadata.columnType === 'EMAIL'"
              :metadata="metadata"
              :data="assertStringValue(data)"
            />

            <ValueHyperlink
              v-else-if="metadata.columnType === 'HYPERLINK'"
              :metadata="metadata"
              :data="assertStringValue(data)"
            />

            <ValueRefBack
              v-else-if="metadata.columnType === 'REFBACK'"
              :metadata="toRefColumn(metadata)"
              :data="assertTableValue(data)"
              @refBackCellClicked="$emit('cellClicked', $event)"
            />

            <ValueFile
              v-else-if="metadata.columnType === 'FILE'"
              :metadata="metadata"
              :data="assertFileValue(data)"
            />
          </template>
          <template v-else>
            <span class="min-h-4 inline-block"></span>
          </template>
        </slot>
        </div>
        <!-- Finder-style middle truncation: keep start and end visible -->
        <div
          v-if="isEllipsisActive"
          class="absolute inset-0 flex items-center"
          aria-hidden="true"
        >
          <span class="truncate min-w-0">{{ truncatedStart }}</span>
          <span class="whitespace-pre flex-shrink-0">{{ truncatedEnd }}</span>
        </div>
      </div>
      <!--
        Floats over the right edge of the text instead of taking its own column, so
        the value uses the full cell width. A fade in the cell background (white, or
        bg-hover on row hover) keeps the text legible underneath the icon.
        Revealed per cell (group/cell) on hover and keyboard focus; always visible on
        touch (pointer-coarse). The fade colour follows the row (unnamed group-hover)
        so it still matches the blue hover background when a sibling cell is hovered.
      -->
      <div
        v-if="isEllipsisActive"
        class="absolute inset-y-0 right-0 flex items-center pl-8 pr-1 bg-gradient-to-l from-[var(--background-color-table)] from-50% to-transparent opacity-0 transition-opacity group-hover:from-[var(--background-color-hover)] group-hover/cell:opacity-100 group-focus-within/cell:opacity-100 focus-within:opacity-100 pointer-coarse:opacity-100"
      >
        <button
          type="button"
          class="flex-shrink-0 text-gray-500 hover:text-gray-900 focus-visible:outline-2 focus-visible:outline-offset-2"
          aria-label="Show full value"
          @click="handleShowMore"
        >
          <BaseIcon name="open-in-full" :width="18" />
        </button>
      </div>
    </div>
  </td>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import type {
  columnValue,
  IColumn,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import {
  assertBooleanValue,
  assertFileValue,
  assertListValue,
  assertNumberValue,
  assertRowValue,
  assertStringValue,
  assertTableValue,
  toRefColumn,
} from "../../utils/typeUtils";
import BaseIcon from "../BaseIcon.vue";
import ValueBool from "../value/Bool.vue";
import ValueDecimal from "../value/Decimal.vue";
import ValueEmail from "../value/Email.vue";
import ValueFile from "../value/File.vue";
import ValueHyperlink from "../value/Hyperlink.vue";
import ValueInt from "../value/Int.vue";
import ValueList from "../value/List.vue";
import ValueLong from "../value/Long.vue";
import ValueObject from "../value/Object.vue";
import ValueRef from "../value/Ref.vue";
import ValueRefBack from "../value/RefBack.vue";
import ValueString from "../value/String.vue";
import ValueText from "../value/Text.vue";

const props = defineProps<{
  metadata?: IColumn;
  data?: columnValue;
}>();

const cellRef = ref<HTMLElement | null>(null);
const isEllipsisActive = ref(false);
const fullText = ref("");
let resizeObserver: ResizeObserver;

// Number of trailing characters kept visible at the end of a truncated cell.
const TRUNCATION_TAIL_LENGTH = 12;

// Above this length a tooltip is the wrong container (use the detail modal instead),
// so the hover tooltip is only offered for short/medium truncated values.
const TOOLTIP_MAX_LENGTH = 120;

// Full value shown in a hover/touch tooltip, but only when the cell is actually
// truncated and short enough to belong in a tooltip. Empty string disables it.
// Keyboard users reach the full value via the focusable expand button + modal.
const tooltipContent = computed(() =>
  isEllipsisActive.value && fullText.value.length <= TOOLTIP_MAX_LENGTH
    ? fullText.value
    : ""
);

const truncatedEnd = computed(() => {
  const text = fullText.value;
  if (!text) return "";
  // Never keep more than half the string as a tail, so the start stays meaningful.
  const tailLength = Math.min(
    TRUNCATION_TAIL_LENGTH,
    Math.floor(text.length / 3)
  );
  return tailLength > 0 ? text.slice(text.length - tailLength) : "";
});

const truncatedStart = computed(() => {
  const text = fullText.value;
  // The start span uses `truncate`, so the "…" is rendered by CSS between the two halves.
  return truncatedEnd.value
    ? text.slice(0, text.length - truncatedEnd.value.length)
    : text;
});

const emit = defineEmits<{
  (e: "cellClicked", payload: cellPayload): void;
}>();

onMounted(async () => {
  await nextTick();
  setIsEllipsisActive();
  if (cellRef.value) {
    resizeObserver = new ResizeObserver(setIsEllipsisActive);
    resizeObserver.observe(cellRef.value);
  }
});

onUnmounted(() => {
  resizeObserver?.disconnect();
});

function setIsEllipsisActive() {
  if (!cellRef.value) {
    isEllipsisActive.value = false;
    return;
  }
  // `cellRef` always holds the full value (kept in layout via `invisible`),
  // so overflow detection stays correct when the column is resized.
  isEllipsisActive.value =
    cellRef.value.offsetWidth < cellRef.value.scrollWidth;
  fullText.value = cellRef.value.textContent?.replace(/\s+/g, " ").trim() ?? "";
}

function handleShowMore() {
  if (props.metadata) {
    emit("cellClicked", {
      data: props.data,
      metadata: props.metadata,
    });
  }
}
</script>
