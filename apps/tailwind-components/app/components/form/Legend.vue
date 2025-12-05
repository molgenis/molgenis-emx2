<template>
  <nav class="pt-4 pb-8 bg-form-legend">
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
          <li class="pl-4 py-2" v-if="header.isVisible">
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

// fallback for the default section
const noSectionsActive = computed(() => {
  return !props.sections.some((section) => section.isActive);
});
</script>
