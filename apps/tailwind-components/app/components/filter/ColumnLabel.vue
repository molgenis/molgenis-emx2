<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  labelParts: string[];
}>();

const isNested = computed(() => props.labelParts.length > 1);
const ariaLabel = computed(() =>
  isNested.value ? props.labelParts.join(" / ") : undefined
);
</script>

<template>
  <span :aria-label="ariaLabel">
    <template v-if="isNested">
      <template v-for="(part, i) in labelParts" :key="i">
        <span>{{ part }}</span
        ><span
          v-if="i < labelParts.length - 1"
          class="text-gray-400"
          aria-hidden="true"
          >&nbsp;→&nbsp;</span
        >
      </template>
    </template>
    <template v-else>{{ labelParts[0] }}</template>
  </span>
</template>
