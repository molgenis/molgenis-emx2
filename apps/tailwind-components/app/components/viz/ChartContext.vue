<script lang="ts" setup>
import type { ChartContext } from "../../../types/viz";

defineProps<ChartContext>();
</script>

<template>
  <div>
    <h3 class="text-title text-heading-4xl" :class="{ 'mb-5': !description }">
      {{ title }}
    </h3>
    <p v-if="description" class="text-title-contrast">{{ description }}</p>
  </div>
</template>
