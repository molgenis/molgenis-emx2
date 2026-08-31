<script setup lang="ts">
import { computed, ref, useId } from "vue";
import type {
  columnValue,
  IRow,
  ITableMetaData,
  LegendGroup,
} from "../../../../metadata-utils/src/types";
import type { cellPayload } from "../../../types/types";
import { flattenObject } from "../../utils/flattenObject";
import {
  groupRecordSections,
  type RecordBox,
  type RecordSection as RecordSectionGroup,
} from "../../utils/groupRecordSections";
import Button from "../Button.vue";
import FormLegend from "../form/Legend.vue";
import InputSearch from "../input/Search.vue";
import RecordPageLayout from "./RecordPageLayout.vue";
import RecordSection from "./RecordSection.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    rowData?: IRow | null;
    showMgColumns?: boolean;
    showMenu?: boolean;
    showCards?: boolean;
    showFilter?: boolean;
    collapsed?: boolean;
  }>(),
  {
    rowData: null,
    showMgColumns: false,
    showMenu: true,
    showCards: true,
    showFilter: false,
    collapsed: false,
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

// Expanding is the reader's call; `collapsed` only sets where they start.
const expanded = ref(!props.collapsed);
const filterId = `display-record-filter-${useId()}`;
const filterValue = ref("");

const sections = computed(() =>
  groupRecordSections(props.metadata, props.rowData, {
    showMgColumns: props.showMgColumns,
    keyColumnsOnly: !expanded.value,
    filterTerm:
      expanded.value && props.showFilter ? filterValue.value : undefined,
  })
);

const boxes = computed<RecordBox[]>(() =>
  sections.value.flatMap((section) => [
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
  ])
);

function hasOwnBox(section: RecordSectionGroup): boolean {
  return !!section.label || section.fields.length > 0;
}

function menuAnchorId(section: RecordSectionGroup): string {
  return hasOwnBox(section)
    ? section.id
    : section.headings[0]?.id ?? section.id;
}

// The menu and the filter belong to the expanded record; collapsed shows neither.
const showLegend = computed(
  () => expanded.value && props.showMenu && boxes.value.length > 1
);
const showFilterBox = computed(() => expanded.value && props.showFilter);

const activeBoxId = ref<string | null>(null);

const legendGroups = computed<LegendGroup[]>(() =>
  sections.value.length === 1
    ? boxes.value.map((box) => ({
        id: box.id,
        label: box.label ?? props.metadata.label,
        href: `#${box.id}`,
        isVisible: true,
        isActive: box.id === activeBoxId.value,
      }))
    : sections.value.map((section) => ({
        id: section.id,
        label: section.label ?? props.metadata.label,
        href: `#${menuAnchorId(section)}`,
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

const recordTitle = computed(() =>
  props.metadata.columns
    .filter((column) => column.key === 1)
    .map((column) => keyValueText(props.rowData?.[column.id]))
    .filter(Boolean)
    .join(" - ")
);

function keyValueText(value: columnValue): string {
  if (value === null || value === undefined) {
    return "";
  }
  return typeof value === "object"
    ? flattenObject(value).trim()
    : String(value);
}
</script>

<template>
  <RecordPageLayout :show-side-nav="showLegend">
    <template v-if="showLegend" #sidebar>
      <FormLegend
        :sections="legendGroups"
        class="hidden lg:block rounded-t-base rounded-b-alt shadow-primary"
      >
        <template v-if="recordTitle" #title>
          <h2
            class="pl-7 mb-6 text-heading-4xl font-display text-title-contrast"
          >
            {{ recordTitle }}
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
        <RecordSection
          v-for="box in boxes"
          :key="box.id"
          :section="box"
          :showCards="showCards"
          :trackInView="showLegend"
          @valueClick="$emit('valueClick', $event)"
          @inView="activeBoxId = box.id"
        />
      </div>
      <div v-if="!expanded" class="pt-7.5">
        <Button
          type="text"
          size="tiny"
          label="Show all fields"
          :aria-expanded="expanded"
          @click="expanded = true"
        />
      </div>
    </template>
  </RecordPageLayout>
</template>
