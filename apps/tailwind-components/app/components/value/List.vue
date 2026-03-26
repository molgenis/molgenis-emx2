<script setup lang="ts">
import { computed } from "vue";
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
  }>(),
  {
    hideListSeparator: false,
  }
);

const emit = defineEmits<{
  (e: "listRefCellClicked", data: ListPayload): void;
}>();

const elementType = computed(
  () => props.metadata.columnType.split("_ARRAY")[0]
);

function handleCellClick() {
  if (!props.data) {
    return;
  }
  emit("listRefCellClicked", { metadata: props.metadata, data: props.data });
}
</script>

<template>
  <template v-for="(listElement, index) in data">
    <ValueString
      v-if="
        elementType === 'STRING' ||
        elementType === 'AUTO_ID' ||
        elementType === 'PERIOD'
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
      @refCellClicked.self="handleCellClick"
    />
    <ValueRefBack
      v-else-if="metadata.columnType === 'REFBACK'"
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
      v-if="Number(data?.length) - 1 !== Number(index) && !hideListSeparator"
    >
      ,&nbsp;
    </span>
  </template>
</template>
