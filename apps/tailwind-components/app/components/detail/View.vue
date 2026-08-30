<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type {
  IRow,
  ITableMetaData,
  LegendGroup,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import {
  groupRecordSections,
  type RecordBox,
  type RecordSection,
} from "../../utils/groupRecordSections";
import FormLegend from "../form/Legend.vue";
import InputSearch from "../input/Search.vue";
import DetailPageLayout from "./PageLayout.vue";
import DetailSection from "./Section.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    rowData?: IRow | null;
    showMgColumns?: boolean;
    showMenu?: boolean;
    showFilter?: boolean;
  }>(),
  {
    rowData: null,
    showMgColumns: false,
    showMenu: true,
    showFilter: true,
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const filterId = `detail-view-filter-${useId()}`;
const filterValue = ref("");

const sections = computed(() =>
  groupRecordSections(props.metadata, props.rowData, {
    showMgColumns: props.showMgColumns,
  })
);

const visibleSections = computed(() =>
  props.showFilter && filterValue.value
    ? filterSections(sections.value, filterValue.value)
    : sections.value
);

// A section and each of its headings are separate boxes; the menu still nests them.
const boxes = computed<RecordBox[]>(() => toBoxes(visibleSections.value));

function toBoxes(sections: RecordSection[]): RecordBox[] {
  return sections.flatMap((section) => [
    ...(hasOwnBox(section)
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
  ]);
}

/** The synthetic top section is nameless, so with every column under a heading it has nothing to show. */
function hasOwnBox(section: RecordSection): boolean {
  return !!section.label || section.fields.length > 0;
}

/** A section that renders no box of its own would otherwise link the menu at a missing id. */
function menuAnchorId(section: RecordSection): string {
  return hasOwnBox(section)
    ? section.id
    : section.headings[0]?.id ?? section.id;
}

// Every box is a jump target, so a section with two headings already needs a menu.
const showLegend = computed(() => props.showMenu && boxes.value.length > 1);

// The filter lives in the sidebar, so its placement ignores the filter's own effect on the boxes.
const showSidebar = computed(
  () => props.showMenu && toBoxes(sections.value).length > 1
);

const legendGroups = computed<LegendGroup[]>(() =>
  visibleSections.value.map((section) => ({
    id: section.id,
    label: section.label ?? props.metadata.label,
    href: `#${menuAnchorId(section)}`,
    isVisible: true,
    headers: section.headings.map((heading) => ({
      id: heading.id,
      label: heading.label,
      href: `#${heading.id}`,
      isVisible: true,
    })),
  }))
);

function filterSections(
  sections: RecordSection[],
  filter: string
): RecordSection[] {
  const needle = filter.toLowerCase();
  const matches = (label: string) => label.toLowerCase().includes(needle);
  return sections
    .map((section) => ({
      ...section,
      fields: section.fields.filter((field) => matches(field.label)),
      headings: section.headings
        .map((heading) => ({
          ...heading,
          fields: heading.fields.filter((field) => matches(field.label)),
        }))
        .filter((heading) => heading.fields.length > 0),
    }))
    .filter(
      (section) => section.fields.length > 0 || section.headings.length > 0
    );
}
</script>

<template>
  <DetailPageLayout :show-side-nav="showSidebar">
    <template v-if="showFilter && !showSidebar" #header>
      <div class="flex pb-[30px]">
        <label :for="filterId" class="sr-only">Filter fields</label>
        <InputSearch
          :id="filterId"
          v-model="filterValue"
          class="w-3/5 xl:w-2/5 2xl:w-1/5"
          placeholder="Filter fields..."
        />
      </div>
    </template>

    <template v-if="showSidebar" #sidebar>
      <FormLegend
        v-if="showLegend"
        :sections="legendGroups"
        class="hidden xl:block"
      />
      <div v-if="showFilter" class="flex pb-[30px] xl:pb-0 xl:pt-5">
        <label :for="filterId" class="sr-only">Filter fields</label>
        <InputSearch
          :id="filterId"
          v-model="filterValue"
          class="w-3/5 xl:w-full"
          placeholder="Filter fields..."
        />
      </div>
    </template>

    <template #main>
      <div class="grid lg:gap-2.5 gap-0">
        <DetailSection
          v-for="box in boxes"
          :key="box.id"
          :section="box"
          @valueClick="$emit('valueClick', $event)"
        />
      </div>
    </template>
  </DetailPageLayout>
</template>
