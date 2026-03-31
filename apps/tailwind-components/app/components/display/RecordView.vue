<script setup lang="ts">
import { computed } from "vue";
import type { IColumnDisplay, ISectionField } from "../../../types/types";
import RecordSection from "./RecordSection.vue";
import DetailPageLayout from "../layout/DetailPageLayout.vue";
import SideNav from "../SideNav.vue";
import {
  isEmptyValue,
  isTopSection,
  getTitleText,
  getSubtitleText,
} from "../../utils/displayUtils";

const props = withDefaults(
  defineProps<{
    columns: IColumnDisplay[];
    data: Record<string, any>;
    showEmpty?: boolean;
    showSideNav?: boolean;
    schemaId?: string;
    rowId?: Record<string, any>;
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
    const isTop = isTopSection(section);
    const sectionDirectColumns = dataColumns.filter(
      (c) => c.section === section.id && !c.heading
    );

    if (
      sectionDirectColumns.length > 0 ||
      headingColumns.some((h) => h.section === section.id)
    ) {
      result.push({
        heading: isTop ? null : section,
        isSection: !isTop,
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

const visibleSections = computed(() =>
  props.showEmpty
    ? sections.value
    : sections.value.filter((s) =>
        s.columns.some((col) => !isEmptyValue(col.value))
      )
);

const navSections = computed(() =>
  visibleSections.value
    .filter((s) => s.heading && !isTopSection(s.heading))
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

const autoTitle = computed(() => getTitleText(props.columns, props.data));
const autoSubtitle = computed(() => getSubtitleText(props.columns, props.data));
</script>

<template>
  <div class="max-w-lg mx-auto lg:px-[30px] px-0">
    <DetailPageLayout :show-side-nav="hasSideNav">
      <template #header>
        <slot v-if="$slots.header" name="header"></slot>
        <div
          v-else-if="autoTitle || autoSubtitle"
          class="flex flex-col px-5 pt-5 pb-6 lg:pb-10 lg:px-0 text-center text-title"
        >
          <h1 class="font-display text-heading-6xl">{{ autoTitle }}</h1>
          <p v-if="autoSubtitle" class="mt-1 text-body-lg">
            {{ autoSubtitle }}
          </p>
        </div>
      </template>
      <template v-if="hasSideNav" #sidebar>
        <SideNav :sections="navSections" :title="navTitle" />
      </template>
      <template #main>
        <div class="grid lg:gap-2.5 gap-0">
          <RecordSection
            v-for="(section, index) in visibleSections"
            :key="section.heading?.id || `orphan-${index}`"
            :heading="section.heading"
            :is-section="section.isSection"
            :columns="section.columns"
            :show-empty="showEmpty"
            :schema-id="schemaId"
            :parent-row-id="rowId"
          />
        </div>

        <slot name="footer"></slot>
      </template>
    </DetailPageLayout>
  </div>
</template>
