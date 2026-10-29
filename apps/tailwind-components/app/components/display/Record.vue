<script setup lang="ts">
import { computed, ref } from "vue";
import {
  isColumnValueObject,
  isFileValue,
  type columnValue,
  type IColumn,
  type IRow,
  type LegendHeading,
  type LegendSection,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import type { RecordListConfig, RecordList } from "../../../types/recordLists";
import type {
  RecordHeading,
  RecordLayout,
  RecordSection,
  RecordSectionGroup,
} from "../../../types/record";
import { groupRecordSections } from "../../utils/groupRecordSections";
import { recordTitle } from "../../utils/recordTitle";
import Legend from "../Legend.vue";
import SidebarLayout from "./SidebarLayout.vue";
import DisplayRecordSection from "./RecordSection.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    row: IRow;
    showTableOfContents?: boolean;
    layout?: RecordLayout;
    titleTemplate?: string;
    lists?: RecordListConfig;
  }>(),
  {
    showTableOfContents: true,
    layout: "CARDS",
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const visibleColumns = computed(() =>
  props.columns.filter(
    (column) =>
      !(props.lists?.[column.id] && hasNoNestedRows(props.row, column.id))
  )
);

function hasNoNestedRows(row: IRow, columnId: string): boolean {
  const aggregate = row[`${columnId}_agg`];
  return (
    isColumnValueObject(aggregate) &&
    !isFileValue(aggregate) &&
    aggregate.count === 0
  );
}

const sections = computed(() =>
  groupRecordSections(visibleColumns.value, props.row)
);

const lists = computed(() => {
  const resolved: Record<string, RecordList> = {};
  for (const column of visibleColumns.value) {
    const entry = props.lists?.[column.id];
    if (entry && column.refTableId && column.refSchemaId) {
      resolved[column.id] = {
        displayConfig: entry,
        schemaId: column.refSchemaId,
        tableId: column.refTableId,
        filter: nestedFilter(column),
      };
    }
  }
  return resolved;
});

function nestedFilter(column: IColumn): Record<string, unknown> {
  if (column.columnType === "REFBACK" && column.refBackId) {
    const key = Object.fromEntries(
      props.columns
        .filter((candidate) => candidate.key === 1)
        .map((candidate) => [candidate.id, props.row[candidate.id]])
    );
    return { [column.refBackId]: { equals: [key] } };
  }
  const value = props.row[column.id];
  const values: columnValue[] = Array.isArray(value)
    ? value
    : value == null
    ? []
    : [value];
  return { _or: values.map(equalsFilter) };
}

function equalsFilter(value: columnValue): unknown {
  return isColumnValueObject(value)
    ? Object.fromEntries(
        Object.entries(value).map(([key, nested]) => [
          key,
          equalsFilter(nested),
        ])
      )
    : { equals: value };
}

const recordSections = computed<RecordSection[]>(() =>
  sections.value.flatMap((section) => [
    ...(hasOwnSection(section)
      ? [
          {
            kind: "section" as const,
            id: section.id,
            label: section.label,
            fields: section.fields,
          },
        ]
      : []),
    ...section.headings.map((heading) => ({
      kind: "heading" as const,
      ...heading,
    })),
  ])
);

function hasOwnSection(section: RecordSectionGroup): boolean {
  return section.fields.length > 0;
}

function legendAnchorId(section: RecordSectionGroup): string {
  return hasOwnSection(section)
    ? section.id
    : section.headings[0]?.id ?? section.id;
}

const hasTableOfContents = computed(
  () => props.showTableOfContents && recordSections.value.length > 1
);

const reportedSectionId = ref<string | null>(null);
const activeSectionId = computed(() => {
  const reported = recordSections.value.some(
    (recordSection) => recordSection.id === reportedSectionId.value
  )
    ? reportedSectionId.value
    : null;
  return reported ?? recordSections.value[0]?.id ?? null;
});

const legendGroups = computed<LegendSection[]>(() => {
  if (sections.value.length === 1) {
    return recordSections.value
      .filter((recordSection) => recordSection.label)
      .map((recordSection) =>
        legendSection(recordSection.id, recordSection.label as string)
      );
  }
  return sections.value.flatMap((section) => {
    if (section.label) {
      return [
        legendSection(
          section.id,
          section.label,
          section.headings.map(legendHeading)
        ),
      ];
    }
    return section.headings.map((heading) =>
      legendSection(heading.id, heading.label)
    );
  });
});

function legendSection(
  id: string,
  label: string,
  headers: LegendHeading[] = []
): LegendSection {
  return {
    id,
    label,
    type: "SECTION",
    headers,
    isActive: id === activeSectionId.value,
  };
}

function legendHeading(heading: RecordHeading): LegendHeading {
  return {
    id: heading.id,
    label: heading.label,
    type: "HEADING",
    isActive: heading.id === activeSectionId.value,
  };
}

function goToSection(id: string): void {
  const group = sections.value.find((section) => section.id === id);
  const targetId = group ? legendAnchorId(group) : id;
  reportedSectionId.value = targetId;
  document.getElementById(targetId)?.scrollIntoView();
}

const title = computed(() =>
  recordTitle(props.columns, props.row, props.titleTemplate)
);
</script>

<template>
  <SidebarLayout :show-sidebar="hasTableOfContents">
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <template v-if="hasTableOfContents" #sidebar>
      <Legend
        :sections="legendGroups"
        class="hidden lg:block rounded-t-base rounded-b-alt shadow-primary"
        @goToSection="goToSection"
      >
        <template v-if="title" #title>
          <h2
            class="pl-7 mb-6 break-words text-heading-4xl font-display text-title-contrast"
          >
            {{ title }}
          </h2>
        </template>
      </Legend>
    </template>

    <template #main>
      <div
        class="grid"
        :class="layout === 'CARDS' ? 'lg:gap-2.5 gap-0' : 'gap-7.5'"
      >
        <DisplayRecordSection
          v-for="recordSection in recordSections"
          :key="recordSection.id"
          :section="recordSection"
          :layout="layout"
          :trackInView="hasTableOfContents"
          :lists="lists"
          @valueClick="$emit('valueClick', $event)"
          @inView="reportedSectionId = recordSection.id"
        >
          <template v-if="$slots.value" #value="{ field }">
            <slot name="value" :field="field" />
          </template>
        </DisplayRecordSection>
      </div>
    </template>
  </SidebarLayout>
</template>
