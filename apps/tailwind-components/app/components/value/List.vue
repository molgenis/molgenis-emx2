<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { isRefbackType } from "../../../../metadata-utils/src";
import ContentClamp from "../ContentClamp.vue";
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
    /** Lines a collapsed list occupies. Leave unset to never collapse. */
    maxLines?: number;
    /** How many values reach the DOM at all. */
    renderLimit?: number;
  }>(),
  {
    hideListSeparator: false,
    renderLimit: 100,
  }
);

const elementType = computed(
  () => props.metadata.columnType.split("_ARRAY")[0]
);

/**
 * Every rendered value stays in the DOM, collapsed or not, so a crawler, in-page
 * search and a screen reader all still reach it. `renderLimit` only stops a huge
 * refback from painting at once; past it, the bound belongs in the query.
 */
const rendered = ref(props.renderLimit);

watch(
  () => [props.renderLimit, props.data] as const,
  () => (rendered.value = props.renderLimit)
);

const displayedData = computed(() => {
  if (!props.data) return props.data;
  return props.data.slice(0, rendered.value);
});

/** True while values exist that the clamp cannot reveal, because they are not rendered. */
const hasUnrendered = computed(
  () => !!props.data && props.data.length > rendered.value
);

function renderMore() {
  rendered.value += props.renderLimit;
}

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
    <ContentClamp
      :maxLines="maxLines"
      :hasMore="hasUnrendered"
      @showMore="renderMore"
    >
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
          >,&nbsp;</span
        >
      </template>
    </ContentClamp>
  </span>
</template>
