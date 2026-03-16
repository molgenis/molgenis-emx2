<template>
  <td class="p-2.5 border-b border-gray-200 min-h-8 text-left truncate">
    <slot name="row-actions"></slot>
    <slot>
      <template v-if="metadata && data !== undefined && data !== null">
        <ValueList
          v-if="isListElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueString
          v-else-if="isStringElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueText
          v-else-if="isTextElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueDecimal
          v-else-if="isDecimalElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueInt
          v-else-if="isIntElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="typeof data === 'number' ? data : Number(data)"
        />

        <ValueRef
          v-else-if="isRefColumn(metadata)"
          :metadata="metadata"
          :data="data"
          @refCellClicked="$emit('cellClicked', $event)"
        />

        <ValueObject
          v-else-if="isObjectElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueBool
          v-else-if="isBoolElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueEmail
          v-else-if="isEmailElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueHyperlink
          v-else-if="isHyperlinkElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />

        <ValueRefBack
          v-else-if="isRefBackColumn(metadata)"
          :metadata="metadata"
          :data="data"
          @refBackCellClicked="$emit('cellClicked', $event)"
        />

        <ValueFile
          v-else-if="isFileElement(metadata.columnType, data)"
          :metadata="metadata"
          :data="data"
        />
        <span v-else>{{ metadata.columnType }}</span>
      </template>
      <template v-else>
        <span></span>
      </template>
    </slot>
  </td>
</template>

<script setup lang="ts">
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { RefPayload } from "../../../types/types";
import {
  isBoolElement,
  isDecimalElement,
  isEmailElement,
  isFileElement,
  isHyperlinkElement,
  isIntElement,
  isListElement,
  isObjectElement,
  isRefBackColumn,
  isRefColumn,
  isStringElement,
  isTextElement,
} from "../../utils/typeChecks";
import ValueBool from "../value/Bool.vue";
import ValueDecimal from "../value/Decimal.vue";
import ValueEmail from "../value/Email.vue";
import ValueFile from "../value/File.vue";
import ValueHyperlink from "../value/Hyperlink.vue";
import ValueInt from "../value/Int.vue";
import ValueList from "../value/List.vue";
import ValueObject from "../value/Object.vue";
import ValueRef from "../value/Ref.vue";
import ValueRefBack from "../value/RefBack.vue";
import ValueString from "../value/String.vue";
import ValueText from "../value/Text.vue";

defineProps<{
  metadata?: IColumn;
  data?: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
