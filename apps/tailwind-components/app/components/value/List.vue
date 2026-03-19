<script setup lang="ts">
import { computed } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import {
  isBoolElement,
  isDecimalElement,
  isEmailElement,
  isHyperlinkElement,
  isIntElement,
  isObjectElement,
  isStringElement,
  isTextElement,
} from "../../utils/typeChecks";
import ValueBool from "./Bool.vue";
import ValueDecimal from "./Decimal.vue";
import ValueEmail from "./Email.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueInt from "./Int.vue";
import ValueObject from "./Object.vue";
import ValueString from "./String.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data: string[] | number[] | Record<string, any>;
    hideListSeparator?: boolean;
  }>(),
  {
    hideListSeparator: false,
  }
);

const elementType = computed(
  () => props.metadata.columnType.split("_ARRAY")[0]
);
</script>

<template>
  <template v-for="(listElement, index) in data">
    <ValueString
      v-if="isStringElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueText
      v-else-if="isTextElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueDecimal
      v-else-if="isDecimalElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueInt
      v-else-if="isIntElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueBool
      v-else-if="isBoolElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueEmail
      v-else-if="isEmailElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueHyperlink
      v-else-if="isHyperlinkElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <ValueObject
      v-else-if="isObjectElement(elementType, listElement)"
      :metadata="metadata"
      :data="listElement"
    />
    <span v-else>{{ elementType }}</span>
    <span
      v-if="Number(data.length) - 1 !== Number(index) && !hideListSeparator"
    >
      ,&nbsp;
    </span>
  </template>
</template>
