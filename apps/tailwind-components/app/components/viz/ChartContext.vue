<script lang="ts" setup>
import type { ChartContext } from "../../../types/viz";

defineProps<ChartContext>();
</script>

<template>
  <div>
    <h3 class="text-heading-2xl" :class="{ 'mb-5': description }">
      {{ title }}
    </h3>
    <p v-if="description">{{ description }}</p>
  </div>
</template>
