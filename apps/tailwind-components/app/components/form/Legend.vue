<template>
  <nav class="bg-form-legend px-12 py-18 mb-18" aria-label="Section navigation">
    <slot name="title" />
    <ul class="list-none">
      <li v-for="(section, index) in sections" :key="section.id">
        <FormLegendHeader
          :id="section.id"
          :label="section.label"
          :isActive="
            section.isActive ? true : false || (noSectionsActive && index === 0)
          "
          :errorCount="section.errorCount"
          @goToSection="emit('goToSection', $event)"
        />
        <ul v-for="header in section.headers" class="list-none">
          <li class="pl-4" v-if="header.isVisible !== false">
            <FormLegendHeader
              :id="header.id"
              :label="header.label"
              :isActive="header.isActive ? true : false"
              :errorCount="header.errorCount"
              @goToSection="emit('goToSection', $event)"
            ></FormLegendHeader>
          </li>
        </ul>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts" setup>
import type { LegendSection } from "../../../../metadata-utils/src/types";
import { computed } from "vue";
import FormLegendHeader from "./legend/Header.vue";

const props = defineProps<{
  sections: LegendSection[];
}>();
const emit = defineEmits(["goToSection"]);

// Fallback for the default section. A nested heading counts as active, or the
// legend lights its first section alongside the heading the reader is actually on.
const noSectionsActive = computed(() => {
  return !props.sections.some(
    (section) =>
      section.isActive || section.headers.some((header) => header.isActive)
  );
});
</script>
