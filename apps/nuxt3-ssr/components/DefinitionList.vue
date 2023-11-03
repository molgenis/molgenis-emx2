<script setup lang="ts">
withDefaults(
  defineProps<{
    small?: boolean;
  }>(),
  {
    small: false,
  }
);

const useGridClasses = "grid md:grid-cols-3 md:gap-2.5";
const smallClasses = "";
</script>

<template>
  <dl class="grid gap-2.5 text-body-base text-gray-900">
    <div :class="small ? smallClasses : useGridClasses">
      <slot></slot>
    </div>
  </dl>
</template>
