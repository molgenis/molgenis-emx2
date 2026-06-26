<template>
  <Button
    :id="`${id}-source-code-toggle`"
    type="text"
    size="small"
    :icon="isExpanded ? 'CaretUp' : 'CaretDown'"
    icon-position="right"
    :aria-controls="`${id}-source-code-content`"
    :aria-expanded="isExpanded"
    @click.prevent="isExpanded = !isExpanded"
  >
    <span v-if="!isExpanded">Show source code</span>
    <span v-else>Hide source code</span>
  </Button>
  <div v-if="isExpanded" :id="`${id}-source-code-content`">
    <pre
      v-if="sourceCode"
      class="mt-4 p-2 bg-code-output text-code-output font-mono"
    >
      {{ sourceCode }}
    </pre>
    <p v-else>No source code found for this page.</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { globKeyToRouteKey } from "../utils/sourceCode";

defineProps<{
  id: string;
}>();

const storyGlob = import.meta.glob("../pages/**/*.vue", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const route = useRoute();
const sourceCode = computed<string>(() => {
  const routeKey = `${route.path}.vue`;
  const matchEntry = Object.entries(storyGlob).find(
    ([globKey]) => globKeyToRouteKey(globKey) === routeKey
  );
  return matchEntry ? matchEntry[1] : "";
});

const isExpanded = ref<boolean>(false);
</script>
