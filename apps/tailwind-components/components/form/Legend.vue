<template>
  <nav class="pt-4 pb-8">
    <h2 class="text-disabled p-4 ml-1">Jump to</h2>
    <ul class="list-none space-y-3">
      <li
        v-for="section in sections"
        class="group flex items-center cursor-pointer"
        @click="emit('goToSection', section.id)"
      >
        <div
          class="h-[24px] w-1 group-hover:bg-button-primary"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <span
          class="pl-4 text-title capitalize"
          :class="{ 'font-bold': section.isActive }"
        >
          {{ section.title }}</span
        >
        <span v-if="(section.errorCount ?? 0) > 0" class="ml-2">
          <div
            class="flex h-5 w-5 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
          >
            {{ section.errorCount }}
          </div>
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
