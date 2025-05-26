<script setup lang="ts">
import type { IColumn, IRefColumn } from "../../../metadata-utils/src/types";
import type { RefPayload } from "../../types/types";
defineProps<{
  metaData: IColumn;
  data: any;
}>();

defineEmits<{
  (e: "valueClick", payload: RefPayload): void;
}>();
</script>

<template>
  <template v-if="data == null || data === undefined"></template>
  <ValueList
    v-else-if="metaData.columnType.endsWith('ARRAY')"
    :metaData="metaData"
    :data="data"
  />

  <ValueString
    v-else-if="metaData.columnType === 'STRING'"
    :metaData="metaData"
    :data="data"
  />

  <ValueText
    v-else-if="metaData.columnType === 'TEXT'"
    :metaData="metaData"
    :data="data"
  />

  <ValueDecimal
    v-else-if="metaData.columnType === 'DECIMAL'"
    :metaData="metaData"
    :data="data"
  />

  <ValueLong
    v-else-if="metaData.columnType === 'LONG'"
    :metaData="metaData"
    :data="typeof data === 'number' ? data : Number(data)"
  />

  <ValueInt
    v-else-if="metaData.columnType === 'INT'"
    :metaData="metaData"
    :data="typeof data === 'number' ? data : Number(data)"
  />

  <ValueRef
    v-else-if="metaData.columnType === 'REF'"
    :metaData="metaData as IRefColumn"
    :data="data"
    @refCellClicked="$emit('valueClick', $event)"
  />

  <ValueObject
    v-else-if="metaData.columnType === 'ONTOLOGY'"
    :metaData="metaData"
    :data="data"
  />

  <ValueBool
    v-else-if="metaData.columnType === 'BOOL'"
    :metaData="metaData"
    :data="data"
  />

  <ValueEmail
    v-else-if="metaData.columnType === 'EMAIL'"
    :metaData="metaData"
    :data="data"
  />

  <ValueHyperlink
    v-else-if="metaData.columnType === 'HYPERLINK'"
    :metaData="metaData"
    :data="data"
  />

  <ValueRefBack
    v-else-if="metaData.columnType === 'REFBACK'"
    :metaData="metaData as IRefColumn"
    :data="data"
    @refBackCellClicked="$emit('valueClick', $event)"
  />

  <ValueFile
    v-else-if="metaData.columnType === 'FILE'"
    :metaData="metaData"
    :data="data"
  />

  <template v-else> {{ metaData.columnType }} </template>
</template>
