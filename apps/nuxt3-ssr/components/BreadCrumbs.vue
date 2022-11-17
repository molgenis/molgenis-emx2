<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
defineProps({
  crumbs: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <div class="xl:hidden text-white flex justify-between">
    <a href="#">
      <span class="sr-only">Go up one level</span>
      <BaseIcon name="arrow-left" />
    </a>
    <a href="#">
      <span class="sr-only">Favorite</span>
      <BaseIcon name="star" />
    </a>
  </div>
  <nav
    class="
      items-center
      justify-center
      hidden
      gap-3
      xl:flex
      font-display
      text-heading-lg
      tracking-widest
    "
  >
    <template v-for="(url, label, index) in crumbs" :key="label">
      <a :href="url" class="text-blue-50 hover:underline">{{ label }}</a>
      <span v-if="index < Object.keys(crumbs).length - 1" class="text-white">
        <BaseIcon name="caret-right" />
      </span>
    </template>
  </nav>
</template>
