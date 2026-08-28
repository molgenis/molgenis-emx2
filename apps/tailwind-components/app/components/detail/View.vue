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

// A menu of one entry helps nobody, so it stays away.
const showSideNav = computed(
  () => props.showMenu && visibleSections.value.length > 1
);

const legendGroups = computed<LegendGroup[]>(() =>
  visibleSections.value.map((section) => ({
    id: section.id,
    label: section.label ?? props.metadata.label,
    href: `#${section.id}`,
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
  <DetailPageLayout :show-side-nav="showSideNav">
    <template v-if="showFilter" #header>
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

    <template v-if="showSideNav" #sidebar>
      <FormLegend :sections="legendGroups" />
    </template>

    <template #main>
      <div class="grid lg:gap-2.5 gap-0">
        <DetailSection
          v-for="section in visibleSections"
          :key="section.id"
          :section="section"
          @valueClick="$emit('valueClick', $event)"
        />
      </div>
    </template>
  </DetailPageLayout>
</template>
