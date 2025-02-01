<template>
  <div>
    <pre v-if="sourceCode">{{ sourceCode }}</pre>
    <div v-else>No source code found for this page</div>
  </div>
</template>

<script setup>
const sourceCodeMap = useRuntimeConfig().public.sourceCodeMap;
const route = useRoute();
const sourceCode = computed(() => sourceCodeMap[route.path + '.vue'] || '');
</script>
