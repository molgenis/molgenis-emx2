<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
defineProps<{
  link: string;
}>();
</script>

<template>
  <div class="lg:hidden text-white flex justify-between px-5 pt-5">
    <NuxtLink :to="link">
      <span class="sr-only">Go up one level</span>
      <BaseIcon name="arrow-left" />
    </NuxtLink>
  </div>
</template>
