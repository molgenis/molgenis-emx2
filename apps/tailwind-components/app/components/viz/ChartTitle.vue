<script lang="ts" setup>
import type { ChartTitle } from "../../../types/viz";
import Heading from "../cms/heading/Heading.vue";
defineProps<ChartTitle>();
</script>

<template>
  <div class="text-title-contrast">
    <Heading :id="`${id}-title`" :text="title" :level="3" class="!mb-0" />
    <p :id="`${id}-description`" v-if="description">{{ description }}</p>
    <slot></slot>
  </div>
</template>
