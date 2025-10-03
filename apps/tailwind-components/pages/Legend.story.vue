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
    label: "Overview",
    id: "overview",
    isActive: true,
    errorCount: 1,
  },
  {
    label: "Population",
    id: "population",
    errorCount: 2,
  },
  {
    label: "Contents",
    id: "contents",
    errorCount: 0,
  },
  {
    label: "Access",
    id: "access",
    errorCount: 0,
  },
  {
    label: "Registration",
    id: "data-registration",
    errorCount: 16,
  },
  {
    label: "Information",
    id: "information",
    errorCount: 0,
  },
]);
</script>
