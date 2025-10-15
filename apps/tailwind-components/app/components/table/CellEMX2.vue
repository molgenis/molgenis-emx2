<template>
  <TableBodyCell>
    <template v-if="data == null || data === undefined"></template>
    <ValueList
      v-else-if="
        metadata.columnType.endsWith('ARRAY') ||
        metadata.columnType === 'MULTISELECT' ||
        metadata.columnType === 'CHECKBOX'
      "
      :metadata="metadata"
      :data="data"
    />

    <ValueString
      v-else-if="
        metadata.columnType === 'STRING' ||
        metadata.columnType === 'DATE' ||
        metadata.columnType === 'DATETIME'
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
      v-else-if="metadata.columnType === 'INT'"
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
      :data="data"
      @refCellClicked="$emit('cellClicked', $event)"
    />

    <ValueObject
      v-else-if="metadata.columnType === 'ONTOLOGY'"
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
      :metadata="metadata as IRefColumn"
      :data="data"
      @refBackCellClicked="$emit('cellClicked', $event)"
    />

    <ValueFile
      v-else-if="metadata.columnType === 'FILE'"
      :metadata="metadata"
      :data="data"
    />

    <template v-else> {{ metadata.columnType }} </template>
    <slot></slot>
  </TableBodyCell>
</template>

<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../../metadata-utils/src/types";
import type { RefPayload } from "../../../types/types";
import TableBodyCell from "./TableBodyCell.vue";
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
import { defineProps, defineEmits } from "vue";

defineProps<{
  metadata: IColumn;
  data: any;
}>();

defineEmits<{
  (e: "cellClicked", payload: RefPayload): void;
}>();
</script>
