<script setup lang="ts">
import type { MenuItem } from "../../types/types";

defineProps<{
  item: MenuItem;
  basePath: string;
  linkClass: string;
}>();
</script>

<template>
  <NuxtLink v-if="item.isSpaLink" :to="item.link" :class="linkClass">
    {{ item.label }}
  </NuxtLink>
  <a v-else :href="basePath + item.link" :class="linkClass">
    {{ item.label }}
  </a>
</template>
