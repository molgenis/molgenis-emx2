<script setup lang="ts">
import { computed, ref, watch } from "vue";
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
    maxItems?: number;
    /** How many more values one click reveals. Keeps a long list from painting at once. */
    step?: number;
  }>(),
  {
    hideListSeparator: false,
    step: 20,
  }
);

const elementType = computed(
  () => props.metadata.columnType.split("_ARRAY")[0]
);

const shown = ref(props.maxItems);

watch(
  () => props.maxItems,
  (value) => (shown.value = value)
);

const displayedData = computed(() => {
  if (!shown.value || !props.data) return props.data;
  return props.data.slice(0, shown.value);
});

/** How many the next click reveals, which is what the control is labelled with. */
const nextCount = computed(() => {
  if (!shown.value || !props.data) return 0;
  return Math.min(props.step, Math.max(0, props.data.length - shown.value));
});

const isExpanded = computed(
  () => !!props.maxItems && !!shown.value && shown.value > props.maxItems
);

function showMore() {
  if (shown.value) shown.value += props.step;
}

function collapse() {
  shown.value = props.maxItems;
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
        v-else-if="elementType === 'INT' || elementType === 'NON_NEGATIVE_INT'"
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
    <button
      v-if="nextCount > 0"
      class="text-link text-body-sm ml-1"
      :aria-expanded="isExpanded"
      @click="showMore"
    >
      +{{ nextCount }} more
    </button>
    <button
      v-if="isExpanded"
      class="text-link text-body-sm ml-1"
      title="Show less"
      aria-label="Show less"
      :aria-expanded="isExpanded"
      @click="collapse"
    >
      less
    </button>
  </span>
</template>
