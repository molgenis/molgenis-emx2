<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import type { ComponentMetaMap } from "../utils/componentMetaTypes";
import componentMetaMapJson from "../../componentMetaMap.json";
import { globKeyToRouteKey } from "../utils/sourceCode";
import { extractTemplateBody } from "../utils/demoSource";

const props = defineProps<{
  title: string;
  description?: string;
}>();

const componentMetaMap = componentMetaMapJson as unknown as ComponentMetaMap;

const resolvedMeta = computed(() => {
  const normalizedTitle = props.title.toLowerCase().replace(/\s/g, "");
  const matchingKey = Object.keys(componentMetaMap).find(
    (key) => key.toLowerCase() === normalizedTitle
  );
  return matchingKey ? componentMetaMap[matchingKey] : null;
});

const route = useRoute();

const storyGlob = import.meta.glob("../pages/**/*.vue", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const pageRawSource = computed<string>(() => {
  const routeKey = `${route.path}.vue`;
  const matchEntry = Object.entries(storyGlob).find(
    ([globKey]) => globKeyToRouteKey(globKey) === routeKey
  );
  return matchEntry ? matchEntry[1] : "";
});

const isNarrative = computed(() => {
  const path = route.path;
  return (
    path === "/patterns" ||
    path.startsWith("/patterns/") ||
    path.endsWith(".other")
  );
});

const hasExplicitDemo = computed(() => pageRawSource.value.includes("<Demo"));

const templateBody = computed(() => extractTemplateBody(pageRawSource.value));
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
    <template v-if="isNarrative || hasExplicitDemo">
      <slot></slot>
    </template>
    <Demo v-else id="page-demo" :source="templateBody">
      <slot></slot>
    </Demo>
    <ApiTable v-if="resolvedMeta" :meta="resolvedMeta" />
  </div>
</template>
