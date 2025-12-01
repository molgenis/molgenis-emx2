<template>
  <div class="flex flex-row">
    <div class="basis-1/2 bg-sidebar-gradient">
      <FormLegend :sections="sections" @go-to-section="handleGoToRequest" />
    </div>
    <div class="basis-1/2 text-title p-4">
      <h3>Active header: {{ activeHeader }}</h3>
      <div class="py-4">mock active element: {{ mockActiveElem }}</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from "vue";
import type {
  LegendHeading,
  LegendSection,
} from "../../../metadata-utils/src/types";

const mockActiveElem = ref("main");

const mainSectioFields = [
  {
    label: "Overview",
    id: "overview",
    errorCount: 1,
    type: "HEADING" as LegendHeading["type"],
    isActive: computed(() => mockActiveElem.value === "overview"),
  },
  {
    label: "Population",
    id: "population",
    errorCount: 2,
    type: "HEADING" as LegendHeading["type"],
    isActive: computed(() => mockActiveElem.value === "population"),
  },
  {
    label: "Contents",
    id: "contents",
    errorCount: 0,
    type: "HEADING" as LegendHeading["type"],
    isActive: computed(() => mockActiveElem.value === "contents"),
  },
];

const accessSectionFields = [
  {
    label: "Registration",
    id: "data-registration",
    errorCount: 16,
    type: "HEADING" as LegendHeading["type"],
    isActive: computed(() => mockActiveElem.value === "data-registration"),
  },
  {
    label: "Information",
    id: "information",
    errorCount: 0,
    type: "HEADING" as LegendHeading["type"],
    isActive: computed(() => mockActiveElem.value === "information"),
  },
];

const sections = ref<LegendSection[]>([
  {
    label: "Main",
    id: "main",
    errorCount: 3,
    type: "SECTION",
    fields: mainSectioFields,
    isActive: computed(
      () =>
        mockActiveElem.value === "main" ||
        mainSectioFields.some((field) => field.isActive.value === true)
    ),
  },
  {
    label: "Access",
    id: "access",
    errorCount: 0,
    type: "SECTION",
    fields: accessSectionFields,
    isActive: computed(
      () =>
        mockActiveElem.value === "access" ||
        accessSectionFields.some((field) => field.isActive.value === true)
    ),
  },
]);

function handleGoToRequest(id: string) {
  mockActiveElem.value = id;
}

const activeHeader = computed(() => {
  for (const section of sections.value) {
    if (section.isActive) {
      return section;
    }
    for (const field of section.fields) {
      if ((field as any).isActive) {
        return field;
      }
    }
  }
  return null;
});
</script>
