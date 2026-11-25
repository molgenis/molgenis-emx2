<script setup lang="ts">
import { computed } from "vue";
import BaseIcon from "./BaseIcon.vue";
import { useTheme } from "../composables/useTheme";

withDefaults(
  defineProps<{
    compact?: boolean;
  }>(),
  {
    compact: false,
  }
);

const { activeTheme, toggleTheme } = useTheme();

const compactLabel = computed(() =>
  activeTheme.value === "light" ? "Switch to dark mode" : "Switch to light mode"
);
</script>
<template>
  <button
    v-if="compact"
    class="flex items-center text-menu transition-colors hover:text-button-toggle-active"
    :aria-label="compactLabel"
    @click="toggleTheme"
  >
    <BaseIcon :name="activeTheme === 'light' ? 'moon' : 'Sun'" :width="24" />
  </button>
  <button
    v-else
    class="relative group w-[72px] border rounded flex items-center cursor-pointer transition-colors"
    @click="toggleTheme"
    aria-label="Toggle theme"
    :aria-pressed="activeTheme === 'dark' ? 'true' : 'false'"
  >
    <span
      :class="
        activeTheme === 'light'
          ? 'text-button-toggle-inactive'
          : 'text-button-toggle-active'
      "
      class="z-10 w-[36px] peer/dark transition-colors hover:text-button-toggle-active"
    >
      <BaseIcon name="moon" :width="36" class="p-2" />
      <span class="sr-only">Light mode</span>
    </span>
    <span
      :class="
        activeTheme === 'light'
          ? 'text-button-toggle-active'
          : 'text-button-toggle-inactive'
      "
      class="z-10 w-[36px] peer/light transition-colors hover:text-button-toggle-active"
    >
      <BaseIcon name="Sun" :width="36" class="p-2" />
      <span class="sr-only">Dark mode</span>
    </span>
    <span
      :class="
        activeTheme === 'dark'
          ? 'left-0 peer-hover/light:text-button-toggle-active peer-hover/light:bg-button-toggle-inactive peer-hover/light:left-1/2'
          : 'left-1/2 peer-hover/dark:text-button-toggle-active peer-hover/dark:bg-button-toggle-inactive peer-hover/dark:left-0'
      "
      class="absolute top-0 bottom-1 h-[36px] w-1/2 rounded border border-gray-400 transition-all duration-700 ease-in-out hover:bg-button-toggle-inactive"
    ></span>
  </button>
</template>
