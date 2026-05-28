<script setup lang="ts">
import { computed } from "vue";
import { NESTED_LABEL_SEPARATOR } from "../../composables/useFilters";

const props = defineProps<{
  label?: string;
  labelParts?: string[];
}>();

const safeParts = computed(() => props.labelParts ?? []);
const isNested = computed(() => safeParts.value.length > 1);
const ariaLabel = computed(() =>
  isNested.value ? safeParts.value.join(NESTED_LABEL_SEPARATOR) : undefined
);
</script>

<template>
  <span :aria-label="ariaLabel">
    <template v-if="isNested">
      <template v-for="(part, i) in safeParts" :key="i">
        <span>{{ part }}</span
        ><span
          v-if="i < safeParts.length - 1"
          class="text-gray-400"
          aria-hidden="true"
          >&nbsp;→&nbsp;</span
        >
      </template>
    </template>
    <template v-else>{{ label }}</template>
  </span>
</template>
