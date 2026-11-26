<script setup lang="ts">
import { computed, ref } from "vue";
import {
  isColumnValueObject,
  isColumnValueObjectArray,
  isFileValue,
  type columnValue,
  type IColumn,
  type IRow,
  type ISchemaMetaData,
  type LegendHeading,
  type LegendSection,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import type {
  ListSeed,
  TableConfig,
  RecordList,
} from "../../../types/recordConfig";
import type {
  RecordHeading,
  RecordLayout,
  RecordSection,
  RecordSectionGroup,
} from "../../../types/record";
import { groupRecordSections } from "../../utils/groupRecordSections";
import { NESTED_LIST_LIMIT } from "../../utils/deriveRecordQuery";
import { recordTitle } from "../../utils/recordTitle";
import type { RowLabel } from "../../utils/displayUtils";
import { equalsFilter, nestedRecordLink } from "../../utils/recordPath";
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
    config?: TableConfig;
    recordPath?: string;
    schema?: ISchemaMetaData;
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
  props.columns.filter((column) => !(column.display && listCount(column) === 0))
);

function listCount(column: IColumn): number | undefined {
  const aggregate = props.row[`${column.id}_agg`];
  return isColumnValueObject(aggregate) &&
    !isFileValue(aggregate) &&
    typeof aggregate.count === "number"
    ? aggregate.count
    : undefined;
}

function listSeed(column: IColumn): ListSeed | undefined {
  const rows = props.row[column.id];
  const count = listCount(column);
  const table = props.schema?.tables.find(
    (candidate) => candidate.id === column.refTableId
  );
  return table &&
    column.refSchemaId === props.schema?.id &&
    count !== undefined &&
    isColumnValueObjectArray(rows)
    ? { columns: table.columns, rows, count }
    : undefined;
}

const sections = computed(() =>
  groupRecordSections(visibleColumns.value, props.row)
);

const lists = computed(() => {
  const resolved: Record<string, RecordList> = {};
  for (const column of visibleColumns.value) {
    if (column.display && column.refTableId && column.refSchemaId) {
      resolved[column.id] = {
        layout: column.display,
        schemaId: column.refSchemaId,
        tableId: column.refTableId,
        filter: nestedFilter(column),
        pageSize: NESTED_LIST_LIMIT,
        seed: listSeed(column),
        rowLabel: rowLabel(column),
        linkTo: listLink(column),
      };
    }
  }
  return resolved;
});

function rowLabel(column: IColumn): RowLabel | undefined {
  const template = column.refLabel ?? column.refLabelDefault;
  return template ? { template, label: column.label } : undefined;
}

function listLink(column: IColumn): ((row: IRow) => string) | undefined {
  const route = props.config?.columns?.[column.id]?.route;
  const target = props.schema?.tables.find(
    (candidate) => candidate.id === column.refTableId
  );
  const recordPath = props.recordPath;
  return route && target && recordPath
    ? (row) => nestedRecordLink(recordPath, column, route, target, row)
    : undefined;
}

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
  return { _or: values.map((item) => equalsFilter(refKey(column, item))) };
}

function refKey(column: IColumn, value: columnValue): columnValue {
  const target = props.schema?.tables.find(
    (candidate) => candidate.id === column.refTableId
  );
  if (
    !target ||
    column.refSchemaId !== props.schema?.id ||
    !isColumnValueObject(value)
  ) {
    return value;
  }
  const keyIds = target.columns
    .filter((candidate) => candidate.key === 1)
    .map((candidate) => candidate.id);
  return Object.fromEntries(
    Object.entries(value).filter(([id]) => keyIds.includes(id))
  );
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
      <div v-if="$slots.intro" class="mb-7.5">
        <slot name="intro" />
      </div>
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
