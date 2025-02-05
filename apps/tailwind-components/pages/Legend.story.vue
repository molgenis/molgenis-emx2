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
    title: "Overview",
    id: "overview",
    isActive: true,
    errorCount: 1,
  },
  {
    title: "Population",
    id: "population",
    errorCount: 2,
  },
  {
    title: "Contents",
    id: "contents",
  },
  {
    title: "Access",
    id: "access",
  },
  {
    title: "Information",
    id: "information",
  },
]);
</script>
