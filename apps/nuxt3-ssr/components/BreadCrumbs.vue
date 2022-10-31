<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import BaseIcon from "./BaseIcon.vue";
</script>

<template>
  <a href="#" class="text-white md:hidden">
    <span class="sr-only">Go up one leven</span>
    <BaseIcon name="arrow-left" />
  </a>
  <nav
    class="items-center justify-center hidden gap-3 md:flex font-display text-heading-lg"
  >
    <a href="#" class="text-white">Home</a>
    <span class="text-white"><BaseIcon name="caret-right" /></span>
    <a href="#" class="text-white">Cohorts</a>
  </nav>
</template>
