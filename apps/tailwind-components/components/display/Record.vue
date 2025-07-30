<script lang="ts" setup>
import { computed } from "vue";
import type { ITableMetaData } from "../../../metadata-utils/src";
import type { recordValue } from "../../../metadata-utils/src/types";

const props = defineProps<{
  tableMetadata?: ITableMetaData;
  inputRowData: recordValue;
  showMgColumns: boolean;
}>();

const filteredTableMetadata = computed(() => {
  return props.tableMetadata?.columns.filter(
    (column) => props.showMgColumns || !column.id.startsWith("mg_")
  );
});

const tableMetadataByHeadings = computed(() => {
  if (filteredTableMetadata.value) {
    const headings = filteredTableMetadata.value.reduce(
      (results, row) => {
        if (row.columnType === "HEADING") {
          results[row.label] = [];
        }

        return results;
      },
      { _base: [] } as Record<string, any>
    );

    let currentHeading: string = "";
    filteredTableMetadata.value?.forEach((row) => {
      if (row.columnType === "HEADING") {
        currentHeading = row.label;
      }

      if (currentHeading === "" && row.columnType !== "HEADING") {
        headings["_base"].push(row);
      } else if (currentHeading !== "" && row.columnType !== "HEADING") {
        headings[currentHeading].push(row);
      } else {
        return null;
      }
    });

    return headings;
  }
});
</script>

<template>
  <div v-for="(headingData, heading) in tableMetadataByHeadings" class="mt-5">
    <p v-if="heading !== '_base'" class="mb-1 text-record-heading font-bold">
      {{ heading }}
    </p>
    <ul>
      <li
        v-for="row in headingData"
        class="grid grid-cols-1 md:grid-cols-[1fr_3fr]"
      >
        <div>
          <span class="text-record-label">{{ row.label }}</span>
        </div>
        <div
          class="text-record-value flex sm:flex-col md:flex-row"
          :class="{
            'md:flex-col': row.columnType.startsWith('HYPERLINK'),
          }"
        >
          <ValueEMX2
            :metadata="row"
            :data="(inputRowData as recordValue)[row.id]"
            :hide-list-separator="
              row.columnType.startsWith('HYPERLINK') ? true : false
            "
          />
        </div>
      </li>
    </ul>
  </div>
</template>
