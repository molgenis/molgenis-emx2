<template>
  <div class="flex flex-row">
    <div class="basis-1/2 bg-sidebar-gradient">
      <FormLegend :sections="sections" @go-to-section="handleGoToRequest" />
    </div>
    <div class="basis-1/2 text-title p-4">
      <h3>Active section: {{ sections.find((s) => s.isActive) }}</h3>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import type { IFormLegendSection } from "../../metadata-utils/src/types";

function handleGoToRequest(id: string) {
  sections.value.forEach((s) => {
    if (s.id === id) {
      s.isActive = true;
    } else {
      s.isActive = false;
    }
  });
}

const sections = ref<IFormLegendSection[]>([
  {
    label: "Main",
    id: "main",
    isActive: true,
    errorCount: 3,
    type: "SECTION",
  },
  {
    label: "Overview",
    id: "overview",
    isActive: true,
    errorCount: 1,
    type: "HEADING",
  },
  {
    label: "Population",
    id: "population",
    errorCount: 2,
    type: "HEADING",
  },
  {
    label: "Contents",
    id: "contents",
    errorCount: 0,
    type: "HEADING",
  },
  {
    label: "Access",
    id: "access",
    errorCount: 0,
    type: "HEADING",
  },
  {
    label: "Registration",
    id: "data-registration",
    errorCount: 16,
    type: "HEADING",
  },
  {
    label: "Information",
    id: "information",
    errorCount: 0,
    type: "HEADING",
  },
]);
</script>
