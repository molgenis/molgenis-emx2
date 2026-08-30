<template>
  <div class="flex flex-row">
    <div class="basis-1/2 bg-sidebar-gradient">
      <h2 class="text-title text-heading-xl px-4">Form sections (click)</h2>
      <FormLegend :sections="sections" @go-to-section="handleGoToRequest" />
    </div>
    <div class="basis-1/2 bg-sidebar-gradient">
      <h2 class="text-title text-heading-xl px-4">Record sections (links)</h2>
      <FormLegend :sections="recordSections">
        <template #title>
          <h3
            class="pl-7 pb-4 text-heading-3xl font-display text-title-contrast"
          >
            spike - dog
          </h3>
        </template>
      </FormLegend>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, type Ref } from "vue";
import type {
  LegendGroup,
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
    isActive: true,
    isVisible: computed(() => true),
  },
  {
    label: "Population",
    id: "population",
    errorCount: computed(() => 2),
    type: "HEADING",
    isActive: false,
    isVisible: computed(() => true),
  },
  {
    label: "Contents",
    id: "contents",
    errorCount: computed(() => 0),
    type: "HEADING",
    isActive: false,
    isVisible: computed(() => true),
  },
];

const accessSectionFields: LegendHeading[] = [
  {
    label: "Registration",
    id: "data-registration",
    errorCount: computed(() => 16),
    type: "HEADING",
    isActive: false,
    isVisible: computed(() => true),
  },
  {
    label: "Information",
    id: "information",
    errorCount: computed(() => 0),
    type: "HEADING",
    isActive: false,
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
    isActive: false,
    isVisible: computed(() => true),
  },
  {
    label: "Access",
    id: "access",
    errorCount: computed(() => 0),
    type: "SECTION",
    headers: accessSectionFields,
    isActive: false,
    isVisible: computed(() => true),
  },
];

const recordSections: LegendGroup[] = [
  {
    label: "Main",
    id: "main",
    href: "#main",
    isVisible: true,
    headers: [
      { label: "Overview", id: "overview", href: "#overview", isVisible: true },
      {
        label: "Population",
        id: "population",
        href: "#population",
        isVisible: true,
      },
    ],
  },
  { label: "Access", id: "access", href: "#access", isVisible: true },
];

function handleGoToRequest(id: string) {
  mockActiveElem.value = id;
}
</script>
