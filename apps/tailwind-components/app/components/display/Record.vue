<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  IColumn,
  IRow,
  LegendGroup,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import type {
  RecordLayout,
  RecordSection,
  RecordSectionGroup,
} from "../../../types/record";
import { groupRecordSections } from "../../utils/groupRecordSections";
import { recordTitle } from "../../utils/recordTitle";
import FormLegend from "../form/Legend.vue";
import RecordPageLayout from "./RecordPageLayout.vue";
import DisplayRecordSection from "./RecordSection.vue";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    row: IRow;
    showLegend?: boolean;
    layout?: RecordLayout;
    titleTemplate?: string;
  }>(),
  {
    showLegend: true,
    layout: "CARDS",
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const sections = computed(() => groupRecordSections(props.columns, props.row));

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
  return !!section.label || section.fields.length > 0;
}

function legendAnchorId(section: RecordSectionGroup): string {
  return hasOwnSection(section)
    ? section.id
    : section.headings[0]?.id ?? section.id;
}

const hasLegend = computed(
  () => props.showLegend && recordSections.value.length > 1
);

const reportedSectionId = ref<string | null>(null);
// RecordSection's rootMargin excludes the page header, so no section reports
// inView at scroll 0, and a row's data can drop the section that did report.
// Either way the first surviving section is the one the reader is on.
const activeSectionId = computed(() => {
  const reported = recordSections.value.some(
    (recordSection) => recordSection.id === reportedSectionId.value
  )
    ? reportedSectionId.value
    : null;
  return reported ?? recordSections.value[0]?.id ?? null;
});

// A section the model never declared has no name of its own, so it gets no entry;
// its headings still do, and they are what a reader navigates by.
const legendGroups = computed<LegendGroup[]>(() =>
  sections.value.length === 1
    ? recordSections.value
        .filter((recordSection) => recordSection.label)
        .map((recordSection) => ({
          id: recordSection.id,
          label: recordSection.label as string,
          isVisible: true,
          isActive: recordSection.id === activeSectionId.value,
        }))
    : sections.value.flatMap((section) => {
        const headers = section.headings.map((heading) => ({
          id: heading.id,
          label: heading.label,
          isVisible: true,
          isActive: heading.id === activeSectionId.value,
        }));
        // An unnamed section contributes its headings directly, rather than an entry with no text.
        return section.label
          ? [
              {
                id: section.id,
                label: section.label,
                isVisible: true,
                isActive: section.id === activeSectionId.value,
                headers,
              },
            ]
          : headers;
      })
);

function goToSection(id: string): void {
  // A section that rendered no box of its own has no element under its own id,
  // so its entry aims at the first heading it did render.
  const group = sections.value.find((section) => section.id === id);
  document.getElementById(group ? legendAnchorId(group) : id)?.scrollIntoView();
}

const title = computed(() =>
  recordTitle(props.columns, props.row, props.titleTemplate)
);
</script>

<template>
  <RecordPageLayout :show-legend="hasLegend">
    <template v-if="hasLegend" #sidebar>
      <FormLegend
        :sections="legendGroups"
        class="hidden lg:block rounded-t-base rounded-b-alt shadow-primary"
        @goToSection="goToSection"
      >
        <template v-if="title" #title>
          <h2
            class="pl-7 mb-6 text-heading-4xl font-display text-title-contrast"
          >
            {{ title }}
          </h2>
        </template>
      </FormLegend>
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
          :trackInView="hasLegend"
          @valueClick="$emit('valueClick', $event)"
          @inView="reportedSectionId = recordSection.id"
        />
      </div>
    </template>
  </RecordPageLayout>
</template>
