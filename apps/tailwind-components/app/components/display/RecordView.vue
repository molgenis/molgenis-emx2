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

  // Find all SECTION columns
  const sectionColumns = columns.filter((c) => c.columnType === "SECTION");

  // Find all HEADING columns
  const headingColumns = columns.filter((c) => c.columnType === "HEADING");

  // Find orphan columns (not in any section or heading)
  const dataColumns = columns.filter(
    (c) => c.columnType !== "SECTION" && c.columnType !== "HEADING"
  );

  const orphanColumns = dataColumns.filter((c) => !c.section && !c.heading);

  // Add orphan columns as first section (no heading)
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

  // Process each SECTION
  for (const section of sectionColumns) {
    // Find columns directly in this section (no heading)
    const sectionDirectColumns = dataColumns.filter(
      (c) => c.section === section.id && !c.heading
    );

    // Add section with its direct columns
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

    // Find headings in this section
    const sectionHeadings = headingColumns.filter(
      (h) => h.section === section.id
    );

    // Process each heading in this section
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

  // Find headings not in any section
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
