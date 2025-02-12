<template>
  <div class="flex justify-start items-start mt-4 gap-8">
    <FormLegend :sections="sections" />
    <div id="form-legend-story-content-container" class="grow">
      <section
        :id="section.id"
        v-for="section in sections"
        class="mb-4 border-b-2 h-40"
      >
        <h2 class="text-heading-xl">{{ section.label }}</h2>
        <p>
          Content for {{ section.label }}. This section has
          <span
            >{{ section.errorCount }}
            {{ section.errorCount === 1 ? "error" : "errors" }}.</span
          >
        </p>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { IFormLegendSection } from "../../metadata-utils/src/types";

const sections: IFormLegendSection[] = [
  { label: "1st Section", id: "section-1", isActive: true, errorCount: 1 },
  { label: "2nd Section", id: "section-2", isActive: false, errorCount: 0 },
  { label: "3rd Section", id: "section-3", isActive: false, errorCount: 16 },
];
</script>
