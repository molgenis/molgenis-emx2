<script setup lang="ts">
import { computed, ref } from "vue";
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
  type RecordSection,
} from "../../utils/groupRecordSections";
import FormLegend from "../form/Legend.vue";
import DetailPageLayout from "./PageLayout.vue";
import DetailSection from "./Section.vue";

const props = withDefaults(
  defineProps<{
    metadata: ITableMetaData;
    rowData?: IRow | null;
    showMgColumns?: boolean;
    showMenu?: boolean;
  }>(),
  {
    rowData: null,
    showMgColumns: false,
    showMenu: true,
  }
);

defineEmits<{
  (e: "valueClick", payload: cellPayload): void;
}>();

const sections = computed(() =>
  groupRecordSections(props.metadata, props.rowData, {
    showMgColumns: props.showMgColumns,
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

function hasOwnBox(section: RecordSection): boolean {
  return !!section.label || section.fields.length > 0;
}

function menuAnchorId(section: RecordSection): string {
  return hasOwnBox(section)
    ? section.id
    : section.headings[0]?.id ?? section.id;
}

const showLegend = computed(() => props.showMenu && boxes.value.length > 1);

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
  <DetailPageLayout :show-side-nav="showLegend">
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
      <div class="grid lg:gap-2.5 gap-0">
        <DetailSection
          v-for="box in boxes"
          :key="box.id"
          :section="box"
          :trackInView="showLegend"
          @valueClick="$emit('valueClick', $event)"
          @inView="activeBoxId = box.id"
        />
      </div>
    </template>
  </DetailPageLayout>
</template>
