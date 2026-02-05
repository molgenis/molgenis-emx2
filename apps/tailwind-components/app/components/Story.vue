<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";

const props = defineProps<{
  title: string;
  description?: string;
  spec?: string;
}>();

const renderedSpec = computed(() => {
  if (!props.spec) return "";
  return marked.parse(props.spec, { async: false }) as string;
});
</script>
<template>
  <div class="px-12 py-4">
    <h1
      v-if="title"
      class="text-heading-6xl text-favorite hover:text-favorite-hover"
    >
      {{ title }}
    </h1>
    <p class="mt-2" v-if="description">{{ description }}</p>
    <slot></slot>
    <section v-if="spec" class="mt-12 p-6 bg-gray-50 rounded border">
      <h2 class="text-heading-lg font-semibold mb-4">Specification</h2>
      <div class="prose prose-sm max-w-none" v-html="renderedSpec" />
    </section>
    <h2 class="text-title text-heading-xl mt-4">Source code:</h2>
    <SourceCode :id="title" />
  </div>
</template>
