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
import { computed, ref, type Ref } from "vue";
import type {
  LegendHeading,
  LegendSection,
} from "../../../metadata-utils/src/types";

const mockActiveElem = ref("main");

const mainSectionFields: LegendHeading[] = [
  {
    label: "Overview",
    id: "overview",
    errorCount: computed(() => 1),
    type: "HEADING",
    isActive: computed(() => mockActiveElem.value === "overview"),
    isVisible: computed(() => true),
  },
  {
    label: "Population",
    id: "population",
    errorCount: computed(() => 2),
    type: "HEADING",
    isActive: computed(() => mockActiveElem.value === "population"),
    isVisible: computed(() => true),
  },
  {
    label: "Contents",
    id: "contents",
    errorCount: computed(() => 0),
    type: "HEADING",
    isActive: computed(() => mockActiveElem.value === "contents"),
    isVisible: computed(() => true),
  },
];

const accessSectionFields: LegendHeading[] = [
  {
    label: "Registration",
    id: "data-registration",
    errorCount: computed(() => 16),
    type: "HEADING",
    isActive: computed(() => mockActiveElem.value === "data-registration"),
    isVisible: computed(() => true),
  },
  {
    label: "Information",
    id: "information",
    errorCount: computed(() => 0),
    type: "HEADING",
    isActive: computed(() => mockActiveElem.value === "information"),
    isVisible: computed(() => true),
  },
];

const sections: LegendSection[] = [
  {
    label: "Main",
    id: "main",
    errorCount: computed(() => 3),
    type: "SECTION",
    headers: mainSectionFields,
    isActive: computed(
      () =>
        mockActiveElem.value === "main" ||
        mainSectionFields.some((field) => field.isActive.value === true)
    ),
    isVisible: computed(() => true),
  },
  {
    label: "Access",
    id: "access",
    errorCount: computed(() => 0),
    type: "SECTION",
    headers: accessSectionFields,
    isActive: computed(
      () =>
        mockActiveElem.value === "access" ||
        accessSectionFields.some((field) => field.isActive.value === true)
    ),
    isVisible: computed(() => true),
  },
];

function handleGoToRequest(id: string) {
  mockActiveElem.value = id;
}

const activeHeader = computed(() => {
  for (const section of sections) {
    if (section.isActive) {
      return section;
    }
    for (const header of section.headers) {
      if ((header as any).isActive) {
        return header;
      }
    }
  }
  return null;
});
</script>
