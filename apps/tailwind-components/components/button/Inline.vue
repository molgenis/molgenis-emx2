<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    icon?: string;
    inverted?: boolean;
  }>(),
  { inverted: false }
);
</script>

<template>
  <button
    class="group flex items-center cursor-pointer disabled:cursor-not-allowed disabled:text-disabled colorCurrent"
  >
    <BaseIcon v-if="icon" :name="icon" :width="18" class="mr-2" />
    <span class="group-enabled:hover:underline">
      <slot />
    </span>
  </button>
</template>
