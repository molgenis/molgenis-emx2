<script setup lang="ts">
withDefaults(
  defineProps<{
    fullScreen?: boolean;
  }>(),
  {
    fullScreen: false,
  }
);
</script>
<template>
  <div
    class="mx-auto lg:px-[30px] px-0"
    :class="fullScreen ? 'max-w-full' : 'max-w-lg'"
  >
    <slot></slot>
  </div>
</template>
