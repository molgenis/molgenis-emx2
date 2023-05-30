<template>
  <div>
    <RowEdit
      v-if="columnsSplitByHeadings"
      :id="id"
      :modelValue="modelValue"
      :pkey="pkey"
      :tableName="tableName"
      :tableMetaData="tableMetaData"
      :schemaMetaData="schemaMetaData"
      :visibleColumns="columnsSplitByHeadings[page - 1]"
      :clone="clone"
      :locale="locale"
      @update:modelValue="$emit('update:modelValue', $event)"
      @errorsInForm="$emit('errorsInForm', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { toRefs } from "vue";
import { ISchemaMetaData } from "../../Interfaces/IMetaData";
import { IRow } from "../../Interfaces/IRow";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import RowEdit from "./RowEdit.vue";

const props = withDefaults(
  defineProps<{
    id: string;
    modelValue: IRow;
    pkey: IRow;
    tableName: string;
    tableMetaData: ITableMetaData;
    schemaMetaData: ISchemaMetaData;
    locale: string;
    columnsSplitByHeadings: string[][];
    clone?: boolean;
    page?: number;
  }>(),
  { page: 1 }
);

const { id, pkey, tableName, schemaMetaData, clone, locale } = props;

let { modelValue, page, tableMetaData, columnsSplitByHeadings } = toRefs(props);
</script>
