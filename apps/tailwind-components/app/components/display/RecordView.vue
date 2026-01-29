<script setup lang="ts">
import { computed } from "vue";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import type { ISectionField } from "../../../types/types";
import RecordSection from "./RecordSection.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    row: Record<string, any>;
    showEmpty?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

interface SectionGroup {
  heading: IColumn | null;
  isSection: boolean;
  columns: ISectionField[];
}

const sections = computed<SectionGroup[]>(() => {
  const columns = props.metadata.columns || [];
  const result: SectionGroup[] = [];

  const sectionColumns = columns.filter((c) => c.columnType === "SECTION");
  const headingColumns = columns.filter((c) => c.columnType === "HEADING");
  const dataColumns = columns.filter(
    (c) => c.columnType !== "SECTION" && c.columnType !== "HEADING"
  );

  const orphanColumns = dataColumns.filter((c) => !c.section && !c.heading);

  if (orphanColumns.length > 0) {
    result.push({
      heading: null,
      isSection: false,
      columns: orphanColumns.map((col) => ({
        meta: col,
        value: props.row[col.id],
      })),
    });
  }

  for (const section of sectionColumns) {
    const sectionDirectColumns = dataColumns.filter(
      (c) => c.section === section.id && !c.heading
    );

    if (
      sectionDirectColumns.length > 0 ||
      headingColumns.some((h) => h.section === section.id)
    ) {
      result.push({
        heading: section,
        isSection: true,
        columns: sectionDirectColumns.map((col) => ({
          meta: col,
          value: props.row[col.id],
        })),
      });
    }

    const sectionHeadings = headingColumns.filter(
      (h) => h.section === section.id
    );

    for (const heading of sectionHeadings) {
      const headingColumns_ = dataColumns.filter(
        (c) => c.heading === heading.id
      );

      if (headingColumns_.length > 0) {
        result.push({
          heading,
          isSection: false,
          columns: headingColumns_.map((col) => ({
            meta: col,
            value: props.row[col.id],
          })),
        });
      }
    }
  }

  const orphanHeadings = headingColumns.filter((h) => !h.section);
  for (const heading of orphanHeadings) {
    const headingColumns_ = dataColumns.filter((c) => c.heading === heading.id);

    if (headingColumns_.length > 0) {
      result.push({
        heading,
        isSection: false,
        columns: headingColumns_.map((col) => ({
          meta: col,
          value: props.row[col.id],
        })),
      });
    }
  }

  return result;
});
</script>

<template>
  <article>
    <slot name="header"></slot>

    <RecordSection
      v-for="(section, index) in sections"
      :key="section.heading?.id || `orphan-${index}`"
      :heading="section.heading"
      :is-section="section.isSection"
      :columns="section.columns"
      :show-empty="showEmpty"
    >
      <template #list="slotProps">
        <slot name="list" v-bind="slotProps" />
      </template>
    </RecordSection>

    <slot name="footer"></slot>
  </article>
</template>
