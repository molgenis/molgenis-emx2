<script setup lang="ts">
withDefaults(
  defineProps<{
    icon: string;
    disabled?: boolean;
    tooltip: string;
  }>(),
  {
    disabled: false,
  }
);

</script>
<template>
  <button
    v-tooltip.top="tooltip"
    class="items-center border-none p-2 font-display text-button-outline duration-default ease-in-out transition-colors"
  >
    <BaseIcon :name="icon" :width="24" />
    <span class="sr-only">{{ tooltip }}<slot /></span>
  </button>
</template>