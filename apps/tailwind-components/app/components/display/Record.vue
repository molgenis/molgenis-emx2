<script lang="ts" setup>
import { computed } from "vue";
import {
  isLayoutColumnType,
  isSectionType,
} from "../../../../metadata-utils/src";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type {
  IColumn,
  recordValue,
} from "../../../../metadata-utils/src/types";
import ValueEMX2 from "../value/EMX2.vue";

const props = withDefaults(
  defineProps<{
    tableMetadata?: ITableMetaData;
    inputRowData: recordValue;
    showMgColumns?: boolean;
  }>(),
  {
    showMgColumns: false,
  }
);

const filteredTableMetadata = computed(() => {
  return props.tableMetadata?.columns.filter(
    (column) => props.showMgColumns || !column.id.startsWith("mg_")
  );
});

interface IRecordSection {
  id: string;
  heading: string;
  headingType: string;
  rows: IColumn[];
}

const recordSections = computed<IRecordSection[]>(() => {
  if (!filteredTableMetadata.value) {
    return [];
  }

  const sections: IRecordSection[] = [];

  filteredTableMetadata.value.forEach((row) => {
    if (isLayoutColumnType(row.columnType)) {
      sections.push({
        id: row.id,
        heading: row.label,
        headingType: row.columnType,
        rows: [],
      });
    } else {
      if (sections.length === 0) {
        sections.push({ id: "", heading: "", headingType: "", rows: [] });
      }
      sections[sections.length - 1]!.rows.push(row);
    }
  });

  return sections;
});
</script>

<template>
  <div v-for="section in recordSections" :key="section.id">
    <p
      v-if="section.heading"
      class="mb-1 text-record-heading font-bold"
      :class="isSectionType(section.headingType) ? 'text-heading-lg' : ''"
    >
      {{ section.heading }}
    </p>
    <ul>
      <li
        v-for="row in section.rows"
        :key="row.id"
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
