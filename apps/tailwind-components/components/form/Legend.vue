<template>
  <nav class="pt-4 pb-8" v-if="sections.length > 1">
    <h3 class="text-disabled p-4 ml-4">Jump to</h3>
    <ul class="list-none space-y-3">
      <li
        v-for="section in sections"
        class="group/chapter flex justify-start items-center gap-1 cursor-pointer [&_div]:border [&_div]:border-red-500"
      >
        <div
          class="h-[24px] w-1 group-hover/chapter:bg-button-primary"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <div class="pl-4">
          <a
            class="relative w-10/12 inline-flex items-center truncate"
            href="#"
            :aria-current="section.isActive"
            @click.prevent="emit('goToSection', section.id)"
          >
            <span
              class="text-title-contrast capitalize truncate"
              :class="{ 'font-bold': section.isActive }"
            >
              {{ section.label }}
            </span>
          </a>
          <div class="w-1/12 inline-flex items-center justify-center">
            <div
              class="inline-flex h-6 w-6 shrink-0 grow-0 items-center justify-center rounded-full bg-notification text-legend-error-count"
            >
              {{ (section.errorCount ?? 0) > 9 ? "9+" : section.errorCount }}
              <span class="sr-only">
                {{ section.errorCount === 1 ? "error" : "errors" }}
              </span>
            </div>
          </div>
        </div>
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
