<script lang="ts" setup>
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { recordValue } from "../../../metadata-utils/src/types";

const props = defineProps<{
  tableMetadata?: ITableMetaData;
  row: recordValue;
  showMgColumns: boolean;
}>();

function headingHasValues(headingId: string, row: recordValue) {
  let inHeading = false;
  let result = false;
  props.tableMetadata?.columns.forEach((column) => {
    if (column.id === headingId) {
      inHeading = true;
    } else if (column.columnType === "HEADING") {
      inHeading = false;
    } else if (inHeading && !column.id.startsWith("mg") && row[column.id]) {
      result = true;
    }
  });

  return result;
}
</script>

<template>
  <template
    v-for="column in tableMetadata?.columns.filter(
      (column) => showMgColumns || !column.id.startsWith('mg_')
    )"
  >
    <template
      v-if="column.columnType === 'HEADING' && headingHasValues(column.id, row)"
    >
      <h3>{{ column.label }}</h3>
    </template>
    <template v-else>
      <ul>
        <li
          v-if="Object.hasOwn(row as recordValue, column.id)"
          class="grid grid-cols-3 justify-start items-center gap-2 flex-col md:flex-row"
        >
          <div class="font-bold text-body-base">
            <span>{{ column.label }}</span>
          </div>
          <div class="col">
            <ValueEMX2
              :metadata="column"
              :data="(row as recordValue)[column.id]"
            />
          </div>
        </li>
      </ul>
    </template>
  </template>
</template>
