<template>
  <nav class="pt-4 pb-8 bg-form-legend" v-if="sections.length > 1">
    <ul class="list-none">
      <li
        v-for="section in sections.filter((section) => section.label)"
        class="py-2 pr-4 relative group/chapter cursor-pointer flex items-center gap-2 justify-start h-full"
      >
        <div
          class="absolute left-0 top-0 h-full w-1 group-hover/chapter:bg-button-primary transition-translate duration-100 ease-in-out origin-left -translate-full group-hover/chapter:translate-0"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <a
          class="pl-7 truncate hover:overflow-visible bg-form-legend"
          href="#"
          :aria-current="section.isActive"
          @click.prevent="emit('goToSection', section.id)"
        >
          <span
            class="text-title-contrast capitalize"
            :class="{
              'font-bold': section.isActive,
              'ml-2 italic': hasSections && section.type === 'HEADING',
            }"
          >
            {{ section.label }}
          </span>
          <span v-if="(section.errorCount ?? 0) > 0" class="sr-only">
            {{ section.errorCount }} error{{
              section.errorCount > 1 || section.errorCount === 0 ? "s" : ""
            }}
          </span>
        </a>
        <div
          v-if="(section.errorCount ?? 0) > 0"
          class="inline-flex h-6 w-6 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
        >
          {{ (section.errorCount ?? 0) > 9 ? "9+" : section.errorCount }}
        </div>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts" setup>
import type { IFormLegendSection } from "../../../metadata-utils/src/types";
import { computed } from "vue";

const props = defineProps<{
  sections: IFormLegendSection[];
}>();
const emit = defineEmits(["goToSection"]);

const hasSections = computed(() => {
  //anonymous sections don't have a label
  return props.sections.some(
    (section) => section.type === "SECTION" && section.label
  );
});
</script>
