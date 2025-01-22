<template>
  <nav class="pt-4 pb-8">
    <h2 class="text-disabled p-4 ml-1">Jump to</h2>
    <ul class="list-none space-y-3">
      <li
        v-for="section in sections"
        class="group felx flex items-center cursor-pointer"
        @click="handleGotoRequest(section)"
      >
        <div
          class="h-[24px] w-1 group-hover:bg-button-primary"
          :class="{ 'bg-button-primary': section.isActive }"
        />
        <span
          class="pl-4 text-title capitalize"
          :class="{ 'font-bold': section.isActive }"
          >{{ section.label }}</span
        >
        <span v-if="(section.errorCount ?? 0) > 0" class="ml-2">
          <a :href="`#${section.domId}`">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-[20px] w-[20] inline-block"
              viewBox="0 0 20 20"
            >
              <circle cx="10" cy="10" r="10" class="fill-notification" />
              <text
                x="10"
                y="10"
                font-size="smaller"
                text-anchor="middle"
                alignment-baseline="central"
                class="stroke-notification-text fill-notification-text"
              >
                {{ section.errorCount }}
              </text>
            </svg>
          </a>
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

const emit = defineEmits(["gotoSection"]);

function handleGotoRequest(section: IFormLegendSection) {
  document.getElementById(`${section.domId}-chapter-title`)?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });

  emit("gotoSection", section);
}
</script>
