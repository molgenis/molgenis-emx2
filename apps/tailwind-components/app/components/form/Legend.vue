<template>
  <nav class="pt-4 pb-8 bg-form-legend" v-if="sections.length > 1">
    <h3 class="text-disabled p-4 ml-4">Jump to</h3>
    <ul class="list-none">
      <li
        v-for="section in sections"
        class="py-2 pr-4 relative group/chapter flex items-center gap-2 justify-start h-full"
      >
        <div
          class="absolute left-0 top-0 h-full w-1 group-hover/chapter:bg-button-primary transition-translate duration-100 ease-in-out origin-left -translate-full group-hover/chapter:translate-0"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <a
          :id="`form-legend-section-${section.id})`"
          :aria-describedby="`form-legend-section-${section.id})-error-count`"
          class="pl-7 truncate hover:overflow-visible bg-form-legend cursor-pointer"
          href="#"
          :aria-current="section.isActive"
          @click.prevent="emit('goToSection', section.id)"
        >
          <span
            class="text-title-contrast capitalize"
            :class="{
              'font-bold': section.isActive,
              'ml-4': hasSections && section.type === 'HEADING',
            }"
          >
            {{ section.label }}
          </span>
        </a>
        <div
          v-if="(section.errorCount ?? 0) > 0"
          :id="`form-legend-section-${section.id}-error-count`"
          class="inline-flex h-6 w-6 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
        >
          <span>{{
            (section.errorCount ?? 0) > 9 ? "9+" : section.errorCount
          }}</span>
          <span class="sr-only">
            error{{
              section.errorCount > 1 || section.errorCount === 0 ? "s" : ""
            }}
            in {{ section.label }}
          </span>
        </div>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts" setup>
import type { IFormLegendSection } from "../../../../metadata-utils/src/types";
import { computed } from "vue";

const props = defineProps<{
  sections: IFormLegendSection[];
}>();
const emit = defineEmits(["goToSection"]);

const hasSections = computed(() => {
  //anonymous sections don't have a label
  return props.sections.some(
    (section) => section.type === "SECTION" && section.id !== "_mg_top_of_form"
  );
});
</script>
