<template>
  <div>
    <pre v-if="sourceCode">{{ sourceCode }}</pre>
    <div v-else>
      No source code found for this page. Might you need to rebuild?
    </div>
  </div>
</template>

<script setup>
const sourceCodeMap = useRuntimeConfig().public.sourceCodeMap;
const route = useRoute();

const sourceCode = computed(() => {
  return sourceCodeMap[route.path + ".vue"] || "";
});
</script>
