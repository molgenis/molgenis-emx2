<script setup lang="ts">
import { computed } from "vue";
import type { IColumnDisplay, ISectionField } from "../../../types/types";
import RecordSection from "./RecordSection.vue";
import DetailPageLayout from "../layout/DetailPageLayout.vue";
import SideNav from "../SideNav.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumnDisplay[];
    data: Record<string, any>;
    showEmpty?: boolean;
    showSideNav?: boolean;
  }>(),
  {
    showEmpty: false,
  }
);

interface SectionGroup {
  heading: IColumnDisplay | null;
  isSection: boolean;
  columns: ISectionField[];
}

const sections = computed<SectionGroup[]>(() => {
  const columns = props.columns;
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
        value: props.data[col.id],
      })),
    });
  }

  for (const section of sectionColumns) {
    const isTopSection =
      section.id === "mg_top_of_form" || section.label === "_top";
    const sectionDirectColumns = dataColumns.filter(
      (c) => c.section === section.id && !c.heading
    );

    if (
      sectionDirectColumns.length > 0 ||
      headingColumns.some((h) => h.section === section.id)
    ) {
      result.push({
        heading: isTopSection ? null : section,
        isSection: !isTopSection,
        columns: sectionDirectColumns.map((col) => ({
          meta: col,
          value: props.data[col.id],
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
            value: props.data[col.id],
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
          value: props.data[col.id],
        })),
      });
    }
  }

  return result;
});

function hasNonEmptyValue(col: ISectionField): boolean {
  const val = col.value;
  if (val === null || val === undefined || val === "") return false;
  if (Array.isArray(val) && val.length === 0) return false;
  if (
    typeof val === "object" &&
    !Array.isArray(val) &&
    Object.keys(val).length === 0
  )
    return false;
  return true;
}

const visibleSections = computed(() =>
  props.showEmpty
    ? sections.value
    : sections.value.filter((s) => s.columns.some(hasNonEmptyValue))
);

const navSections = computed(() =>
  visibleSections.value
    .filter(
      (s) =>
        s.heading &&
        s.heading.id !== "_top" &&
        s.heading.id !== "mg_top_of_form" &&
        s.heading.label !== "_top"
    )
    .map((s) => ({
      id: s.heading!.id,
      label: s.heading!.label || s.heading!.id,
    }))
);

const navTitle = computed(() => {
  const keyColumns = props.columns
    .filter((c) => c.key === 1)
    .map((c) => props.data[c.id])
    .filter(Boolean);
  return keyColumns.join(" - ") || undefined;
});

const hasSideNav = computed(
  () => props.showSideNav !== false && navSections.value.length > 0
);
</script>

<template>
  <div class="lg:px-[30px] px-0">
  <DetailPageLayout :show-side-nav="hasSideNav">
    <template #header>
      <slot name="header"></slot>
    </template>
    <template v-if="hasSideNav" #sidebar>
      <SideNav :sections="navSections" :title="navTitle" />
    </template>
    <template #main>
      <RecordSection
        v-for="(section, index) in visibleSections"
        :key="section.heading?.id || `orphan-${index}`"
        :heading="section.heading"
        :is-section="section.isSection"
        :columns="section.columns"
        :show-empty="showEmpty"
      />

      <slot name="footer"></slot>
    </template>
  </DetailPageLayout>
  </div>
</template>
