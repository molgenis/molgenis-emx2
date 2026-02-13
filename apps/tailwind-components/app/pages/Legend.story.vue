<template>
  <div class="flex flex-row">
    <div class="basis-1/2 bg-sidebar-gradient">
      <FormLegend :sections="sections" @go-to-section="handleGoToRequest" />
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

function handleGoToRequest(id: string) {
  mockActiveElem.value = id;
}
</script>
