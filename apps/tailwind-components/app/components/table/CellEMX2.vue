<template>
  <td class="p-2.5 border-b border-gray-200 min-h-8 text-left truncate">
    <slot name="row-actions"></slot>
    <slot>
      <template v-if="metadata && data !== undefined && data !== null">
        <span :ref="`blaat-${metadata?.id}-${data}`">
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
            :metadata="metadata as IRefColumn"
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

          <span v-if="data && isEllipsisActive()">
            <Button type="inline" size="tiny" @click="handleShowMore">
              Show more
            </Button>
          </span>
        </span>
      </template>
      <template v-else>
        <span class="min-h-4 inline-block"></span>
      </template>
    </slot>
  </td>
</template>

<script setup lang="ts">
import type {
  columnValue,
  IColumn,
  IRefColumn,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import ValueList from "../value/List.vue";
import ValueString from "../value/String.vue";
import ValueText from "../value/Text.vue";
import ValueDecimal from "../value/Decimal.vue";
import ValueLong from "../value/Long.vue";
import ValueInt from "../value/Int.vue";
import ValueRef from "../value/Ref.vue";
import ValueObject from "../value/Object.vue";
import ValueBool from "../value/Bool.vue";
import ValueEmail from "../value/Email.vue";
import ValueHyperlink from "../value/Hyperlink.vue";
import ValueRefBack from "../value/RefBack.vue";
import ValueFile from "../value/File.vue";
import {
  assertListValue,
  assertStringValue,
  assertNumberValue,
  assertBooleanValue,
  assertRowValue,
  assertTableValue,
  assertFileValue,
  toRefColumn,
} from "../../utils/typeUtils";

const props = defineProps<{
  metadata?: IColumn;
  data?: columnValue;
}>();

const emit = defineEmits<{
  (e: "cellClicked", payload: cellPayload): void;
}>();

function handleShowMore() {
  if (props.metadata) {
    emit("cellClicked", {
      data: props.data,
      metadata: props.metadata,
    });
  }
}

function isEllipsisActive() {
  const cellRef = `blaat-${props.metadata?.id}-${props.data}`;
  const cellElement = document.getElementById(cellRef);
  if (cellElement) {
    console.log("Checking ellipsis for cell:", cellElement);
    console.log(
      "OffsetWidth:",
      cellElement.offsetWidth,
      "ScrollWidth:",
      cellElement.scrollWidth
    );
    return cellElement
      ? cellElement.offsetWidth < cellElement.scrollWidth
      : false;
  }
}
</script>
