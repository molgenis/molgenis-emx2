<template>
  <nav class="pt-4 pb-8">
    <h3 class="text-disabled p-4 ml-1">Jump to</h3>
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
          class="pl-4 text-title capitalize"
          :class="{ 'font-bold': section.isActive }"
          href="#"
          @click.prevent="emit('goToSection', section.id)"
        >
          {{ section.label }}
        </a>
        <span
          v-if="(section.errorCount ?? 0) > 0"
          class="ml-2 flex h-5 w-5 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
        >
          {{ section.errorCount }}
        </span>
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
