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
import { Ref, computed, toRefs } from "vue";
import { IColumn } from "../../Interfaces/IColumn";
import { ISchemaMetaData } from "../../Interfaces/IMetaData";
import { IRow } from "../../Interfaces/IRow";
import { ITableMetaData } from "../../Interfaces/ITableMetaData";
import RowEdit from "./RowEdit.vue";

console.log("asdfad");
const emit = defineEmits(["setPageCount", "update:modelValue", "errorsInForm"]);
const props = withDefaults(
  defineProps<{
    id: string;
    modelValue: IRow;
    pkey: IRow;
    tableName: string;
    tableMetaData: ITableMetaData;
    schemaMetaData: ISchemaMetaData;
    visibleColumns: string[] | null;
    locale: string;
    clone?: boolean;
    page?: number;
  }>(),
  { page: 1 }
);

const { id, pkey, tableName, schemaMetaData, visibleColumns, clone, locale } =
  props;

let { modelValue, page, tableMetaData } = toRefs(props);

let columnsSplitByHeadings: Ref<string[][]> = computed(() => {
  const split = splitColumnNamesByHeadings(
    filterVisibleColumns(tableMetaData.value?.columns || [], visibleColumns)
  );
  emit("setPageCount", split.length);
  console.log(visibleColumns);
  return split;
});

function filterVisibleColumns(
  columns: IColumn[],
  visibleColumns: string[] | null
) {
  if (!visibleColumns) {
    return columns;
  } else {
    return columns.filter((column) => visibleColumns.includes(column.name));
  }
}

function splitColumnNamesByHeadings(columns: IColumn[]): string[][] {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([column.name]);
    } else {
      if (accum.length === 0) {
        accum.push([] as string[]);
      }
      accum[accum.length - 1].push(column.name);
    }
    return accum;
  }, [] as string[][]);
}
</script>
