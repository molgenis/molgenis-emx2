<script setup lang="ts">
import { useHead, useCookie } from "#app";
const activeTheme = useCookie("theme", {
  default: () => {
    if (import.meta.client) {
      return window.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
    } else {
      return "light";
    }
  },
});

useHead({
  htmlAttrs: {
    "data-theme": activeTheme,
  },
});

function toggleTheme() {
  activeTheme.value = activeTheme.value === "light" ? "dark" : "light";
}
</script>
<template>
  <div
    class="relative group w-[72px] border rounded flex items-center cursor-pointer transition-colors"
    @click="toggleTheme"
  >
    <div
      :class="
        activeTheme === 'light'
          ? 'text-button-toggle-inactive'
          : 'text-button-toggle-active'
      "
      class="z-10 w-[36px] peer/dark transition-colors hover:text-button-toggle-active"
    >
      <BaseIcon name="moon" :width="36" class="p-2" />
    </div>
    <div
      :class="
        activeTheme === 'light'
          ? 'text-button-toggle-active'
          : 'text-button-toggle-inactive'
      "
      class="z-10 w-[36px] peer/light transition-colors hover:text-button-toggle-active"
    >
      <BaseIcon name="sun" :width="36" class="p-2" />
    </div>
    <div
      :class="
        activeTheme === 'dark'
          ? 'left-0 peer-hover/light:text-button-toggle-active peer-hover/light:bg-button-toggle-inactive peer-hover/light:left-1/2'
          : 'left-1/2 peer-hover/dark:text-button-toggle-active peer-hover/dark:bg-button-toggle-inactive peer-hover/dark:left-0'
      "
      class="absolute top-0 bottom-1 h-[36px] w-1/2 rounded border border-gray-400 transition-all duration-700 ease-in-out hover:bg-button-toggle-inactive"
    ></div>
  </div>
</template>
