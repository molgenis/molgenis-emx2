<template>
  <Button
    :id="`${id}-source-code-toggle`"
    size="tiny"
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
    <p v-else>No source code found for this page. Might you need to rebuild?</p>
  </div>
</template>

<script setup lang="ts">
import { useRuntimeConfig } from "#app";
import { computed, ref } from "vue";
import { useRoute } from "vue-router";

defineProps<{
  id: string;
}>();

interface ISourceCodeMap {
  [key: string]: string;
}

const sourceCodeMap: ISourceCodeMap = useRuntimeConfig().public
  .sourceCodeMap as ISourceCodeMap;
const route = useRoute();
const sourceCode = computed<string>(() => {
  return sourceCodeMap[`${route.path}.vue` as string] || "";
});

const isExpanded = ref<boolean>(false);
</script>
