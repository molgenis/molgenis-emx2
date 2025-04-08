<script setup lang="ts">
withDefaults(
  defineProps<{
    value: number;
    label?: string;
    suffix?: string;
    valuePrefix?: string | null;
  }>(),
  {
    value: 0,
    label: "result",
    suffix: "s",
    valuePrefix: null,
  }
);
</script>

<template>
  <div class="mt-1 mb-0 lg:mb-3 text-body-lg flex flex-col text-title">
    <p class="search-results-count">
      {{ valuePrefix }} {{ value }}
      {{ value == 1 ? label.substring(0, label.length - 1) : label }}
    </p>
  </div>
</template>
