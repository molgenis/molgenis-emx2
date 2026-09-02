<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type {
  IRow,
  ITableMetaData,
  LegendGroup,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import type { RecordSection, RecordSectionGroup } from "../../../types/record";
import { groupRecordSections } from "../../utils/groupRecordSections";
import { recordTitle } from "../../utils/recordTitle";
import FormLegend from "../form/Legend.vue";
import InputSearch from "../input/Search.vue";
import RecordPageLayout from "./RecordPageLayout.vue";
import DisplayRecordSection from "./RecordSection.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    rowData?: IRow | null;
    showMgColumns?: boolean;
    showLegend?: boolean;
    showCards?: boolean;
    showFilter?: boolean;
  }>(),
  {
    rowData: null,
    showMgColumns: false,
    showLegend: true,
    showCards: true,
    showFilter: false,
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const filterId = `display-record-filter-${useId()}`;
const filterValue = ref("");

const sections = computed(() =>
  groupRecordSections(props.metadata, props.rowData, {
    showMgColumns: props.showMgColumns,
    filterTerm: props.showFilter ? filterValue.value : undefined,
  })
);

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

// Enough sections to need navigating, and the caller has not turned the legend off.
const hasLegend = computed(
  () => props.showLegend && recordSections.value.length > 1
);
const showFilterBox = computed(() => props.showFilter);

const activeBoxId = ref<string | null>(null);

const legendGroups = computed<LegendGroup[]>(() =>
  sections.value.length === 1
    ? recordSections.value.map((box) => ({
        id: box.id,
        label: box.label ?? props.metadata.label,
        href: `#${box.id}`,
        isVisible: true,
        isActive: box.id === activeBoxId.value,
      }))
    : sections.value.map((section) => ({
        id: section.id,
        label: section.label ?? props.metadata.label,
        href: `#${legendAnchorId(section)}`,
        isVisible: true,
        isActive: section.id === activeBoxId.value,
        headers: section.headings.map((heading) => ({
          id: heading.id,
          label: heading.label,
          href: `#${heading.id}`,
          isVisible: true,
          isActive: heading.id === activeBoxId.value,
        })),
      }))
);

const title = computed(() => recordTitle(props.metadata, props.rowData));
</script>

<template>
  <RecordPageLayout :show-legend="hasLegend">
    <template v-if="hasLegend" #sidebar>
      <FormLegend
        :sections="legendGroups"
        class="hidden lg:block rounded-t-base rounded-b-alt shadow-primary"
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
      <div v-if="showFilterBox" class="pb-7.5">
        <label :for="filterId" class="sr-only">Filter fields</label>
        <InputSearch
          :id="filterId"
          v-model="filterValue"
          class="w-3/5 lg:w-2/5"
          placeholder="Filter fields..."
        />
      </div>
      <div class="grid" :class="showCards ? 'lg:gap-2.5 gap-0' : 'gap-7.5'">
        <DisplayRecordSection
          v-for="box in recordSections"
          :key="box.id"
          :section="box"
          :showCards="showCards"
          :trackInView="hasLegend"
          @valueClick="$emit('valueClick', $event)"
          @inView="activeBoxId = box.id"
        />
      </div>
    </template>
  </RecordPageLayout>
</template>
