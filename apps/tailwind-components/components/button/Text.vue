<script setup lang="ts">
withDefaults(
  defineProps<{
    inverted: boolean;
    icon?: string;
  }>(),
  {
    inverted: false,
  }
);
</script>

<template>
  <button class="flex items-center">
    <BaseIcon
      v-if="icon"
      :name="icon"
      :class="`text-search-filter-expand${inverted ? '-mobile' : ''}`"
      :width="18"
    />
    <span
      class="ml-2 text-body-sm hover:underline"
      :class="`text-search-filter-expand${inverted ? '-mobile' : ''}`"
    >
      <slot />
    </span>
  </button>
</template>
