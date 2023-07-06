<template>
  <div class="tabs-content" v-show="selectedTitle === title">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { inject } from "vue";
defineProps({
  title: String,
});
const selectedTitle = inject("selectedTitle");
</script>
