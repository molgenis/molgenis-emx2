<template>
  <nav class="pt-4 pb-8" v-if="sections.length > 1">
    <h3 class="text-disabled p-4 ml-4">Jump to</h3>
    <ul class="list-none space-y-3">
      <li
        v-for="section in sections"
        class="group flex items-center cursor-pointer"
      >
        <div
          class="h-[24px] w-1 group-hover:bg-button-primary"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <a
          class="pl-7 flex items-center"
          href="#"
          :aria-current="section.isActive"
          @click.prevent="emit('goToSection', section.id)"
        >
          <span
            class="text-title-contrast capitalize"
            :class="{ 'font-bold': section.isActive }"
          >
            {{ section.label }}
          </span>
          <span
            v-if="(section.errorCount ?? 0) > 0"
            class="ml-2 flex h-6 w-6 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
          >
            {{ (section.errorCount ?? 0) > 9 ? "9+" : section.errorCount }}
            <span class="sr-only">{{
              section.errorCount === 1 ? "error" : "errors"
            }}</span>
          </span>
        </a>
      </li>
    </ul>
  </nav>
</template>

<script lang="ts" setup>
import type { IFormLegendSection } from "../../../metadata-utils/src/types";

defineProps<{
  sections: IFormLegendSection[];
}>();
const emit = defineEmits(["goToSection"]);
</script>
