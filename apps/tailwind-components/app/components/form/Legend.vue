<template>
  <nav class="pt-4 pb-8 bg-form-legend">
    <ul class="list-none">
      <li v-for="(section, index) in sectionsWithHeaders" :key="section.id">
        <FormLegendHeader
          :id="section.id"
          :label="section.label"
          :isActive="section.isActive || (noSectionsActive && index === 0)"
          :errorCount="section.errorCount"
          @goToSection="emit('goToSection', $event)"
        />
        <ul v-for="header in section.headers" class="list-none">
          <li class="pl-4 py-2">
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
import type {
  LegendHeading,
  LegendSection,
} from "../../../../metadata-utils/src/types";
import { computed } from "vue";
import FormLegendHeader from "./legend/Header.vue";

const props = defineProps<{
  sections: LegendSection[];
}>();
const emit = defineEmits(["goToSection"]);

const sectionsWithHeaders = computed(() => {
  return props.sections.map((section) => {
    return {
      id: section.id,
      label: section.label,
      type: section.type,
      isActive: section.isActive ? true : false,
      errorCount: section.errorCount ?? 0,
      headers: section.fields.filter((field) =>
        field.hasOwnProperty("type")
      ) as LegendHeading[],
    };
  });
});

// fallback for the dedault section
const noSectionsActive = computed(() => {
  return !props.sections.some((section) => section.isActive);
});
</script>
