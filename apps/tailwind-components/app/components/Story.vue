<script setup lang="ts">
import { computed } from "vue";
import type { ComponentMetaMap } from "../utils/componentMetaTypes";
import componentMetaMapJson from "../../componentMetaMap.json";

const props = defineProps<{
  title: string;
  description?: string;
  showSource?: boolean;
}>();

const showSourcePanel = computed(() => props.showSource !== false);

const componentMetaMap = componentMetaMapJson as unknown as ComponentMetaMap;

const resolvedMeta = computed(() => {
  const normalizedTitle = props.title.toLowerCase().replace(/\s/g, "");
  const matchingKey = Object.keys(componentMetaMap).find(
    (key) => key.toLowerCase() === normalizedTitle
  );
  return matchingKey ? componentMetaMap[matchingKey] : null;
});
</script>

<template>
  <div class="px-12 py-4 overflow-auto h-full">
    <h1
      v-if="title"
      class="text-heading-6xl text-favorite hover:text-favorite-hover"
    >
      {{ title }}
    </h1>
    <p class="mt-2" v-if="description">{{ description }}</p>
    <slot></slot>
    <ApiTable v-if="resolvedMeta" :meta="resolvedMeta" />
    <template v-if="showSourcePanel">
      <h2 class="text-title text-heading-xl mt-4">Source code:</h2>
      <SourceCode :id="title" />
    </template>
  </div>
</template>
