<template>
  <nav class="pt-4 pb-8 bg-form-legend" aria-label="Section navigation">
    <ul class="list-none">
      <li v-for="(section, index) in sections" :key="section.id">
        <FormLegendHeader
          :id="section.id"
          :idPrefix="idPrefix"
          :label="section.label"
          :href="section.href"
          :isActive="
            section.isActive
              ? true
              : false || (noSectionsActive && index === 0 && !section.href)
          "
          :errorCount="section.errorCount"
          @goToSection="emit('goToSection', $event)"
        />
        <ul v-for="header in section.headers" class="list-none">
          <li class="pl-4 py-2" v-if="header.isVisible !== false">
            <FormLegendHeader
              :id="header.id"
              :idPrefix="idPrefix"
              :label="header.label"
              :href="header.href"
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
import type { LegendGroup } from "../../../../metadata-utils/src/types";
import { computed, useId } from "vue";
import FormLegendHeader from "./legend/Header.vue";

const props = defineProps<{
  sections: LegendGroup[];
}>();
const emit = defineEmits(["goToSection"]);

const idPrefix = `form-legend-header-${useId()}`;

// fallback for the default section
const noSectionsActive = computed(() => {
  return !props.sections.some((section) => section.isActive);
});
</script>
