<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import type { ListPayload, RefPayload } from "../../../types/types";
import { toRefColumn } from "../../utils/typeUtils";
import ValueBool from "./Bool.vue";
import ValueDecimal from "./Decimal.vue";
import ValueEmail from "./Email.vue";
import ValueFile from "./File.vue";
import ValueHyperlink from "./Hyperlink.vue";
import ValueInt from "./Int.vue";
import ValueList from "./List.vue";
import ValueLong from "./Long.vue";
import ValueObject from "./Object.vue";
import ValueRef from "./Ref.vue";
import ValueRefBack from "./RefBack.vue";
import ValueString from "./String.vue";
import ValueText from "./Text.vue";
import ValueDate from "./Date.vue";
import ValueDateTime from "./DateTime.vue";

withDefaults(
  defineProps<{
    metadata: IColumn;
    data: any;
    hideListSeparator?: boolean;
  }>(),
  {
    hideListSeparator: false,
  }
);

defineEmits<{
  (e: "valueClick", payload: RefPayload | ListPayload): void;
}>();
</script>

<template>
  <template v-if="data == null || data === undefined"></template>
  <ValueList
    v-else-if="
      metadata.columnType.endsWith('ARRAY') ||
      metadata.columnType === 'CHECKBOX' ||
      metadata.columnType === 'MULTISELECT'
    "
    :metadata="metadata"
    :data="data"
    :hideListSeparator="hideListSeparator"
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
    v-else-if="['REF', 'RADIO', 'SELECT'].includes(metadata.columnType)"
    :metadata="metadata as IRefColumn"
    :data="data"
    @refCellClicked="$emit('valueClick', $event)"
  />

  <ValueObject
    v-else-if="['ONTOLOGY'].includes(metadata.columnType)"
    :metadata="metadata"
    :data="data"
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

  <ValueRefBack
    v-else-if="metadata.columnType === 'REFBACK'"
    :metadata="toRefColumn(metadata)"
    :data="data"
    @refBackCellClicked="$emit('valueClick', $event)"
  />

  <ValueFile
    v-else-if="metadata.columnType === 'FILE'"
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
