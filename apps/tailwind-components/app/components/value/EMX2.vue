<script setup lang="ts">
import {
  isFileType,
  isSingleOntologyType,
  isSingleRefType,
  isMultiValuedType,
} from "../../../../metadata-utils/src";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import { toRefColumn } from "../../utils/typeUtils";
import ValueBool from "./Bool.vue";
import ValueDate from "./Date.vue";
import ValueDateTime from "./DateTime.vue";
import ValueDecimal from "./Decimal.vue";
import ValueEmail from "./Email.vue";
import ValueFile from "./File.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueInt from "./Int.vue";
import ValueList from "./List.vue";
import ValueLong from "./Long.vue";
import ValueObject from "./Object.vue";
import ValueRef from "./Ref.vue";
import ValueString from "./String.vue";
import ValueText from "./Text.vue";

const props = withDefaults(
  defineProps<{
    metadata: IColumn;
    data: any;
    hideListSeparator?: boolean;
    maxLines?: number;
    renderLimit?: number;
    truncate?: boolean;
  }>(),
  {
    hideListSeparator: false,
    truncate: true,
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();
</script>

<template>
  <template v-if="data == null || data === undefined"></template>
  <ValueList
    v-else-if="isMultiValuedType(metadata.columnType)"
    :metadata="metadata"
    :data="data"
    :hideListSeparator="hideListSeparator"
    :maxLines="maxLines"
    :truncate="truncate"
    :renderLimit="renderLimit"
    @listRefCellClicked="$emit('valueClick', $event)"
  />

  <ValueString
    v-else-if="
      ['STRING', 'AUTO_ID', 'UUID', 'PERIOD'].includes(metadata.columnType)
    "
    :metadata="metadata"
    :data="data"
  />

  <ValueText
    v-else-if="metadata.columnType === 'TEXT'"
    :metadata="metadata"
    :data="data"
    :maxLines="maxLines"
    :truncate="truncate"
  />

  <ValueDecimal
    v-else-if="metadata.columnType === 'DECIMAL'"
    :metadata="metadata"
    :data="data"
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
    v-else-if="isSingleRefType(metadata.columnType)"
    :metadata="toRefColumn(metadata)"
    :data="data"
    @refCellClicked="$emit('valueClick', $event)"
  />

  <ValueObject
    v-else-if="isSingleOntologyType(metadata.columnType)"
    :metadata="metadata"
    :data="data"
    @refCellClicked="$emit('valueClick', $event)"
  />

  <ValueBool
    v-else-if="metadata.columnType === 'BOOL'"
    :metadata="metadata"
    :data="data"
  />

  <ValueEmail
    v-else-if="metadata.columnType === 'EMAIL'"
    :metadata="metadata"
    :data="data"
  />

  <ValueHyperlink
    v-else-if="metadata.columnType === 'HYPERLINK'"
    :metadata="metadata"
    :data="data"
  />

  <ValueFile
    v-else-if="isFileType(metadata.columnType)"
    :metadata="metadata"
    :data="data"
  />

  <ValueDate
    v-else-if="metadata.columnType === 'DATE'"
    :metadata="metadata"
    :data="data"
  />

  <ValueDateTime
    v-else-if="metadata.columnType === 'DATETIME'"
    :metadata="metadata"
    :data="data"
  />

  <template v-else> {{ metadata.columnType }} </template>
</template>
