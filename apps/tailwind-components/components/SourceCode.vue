<template>
  <div v-if="show">
    <Button size="tiny" @click.prevent="toggle">hide source code</Button>
    <pre v-if="sourceCode">{{ sourceCode }}</pre>
    <div v-else>
      No source code found for this page. Might you need to rebuild?
    </div>
  </div>
  <div v-else>
    <Button @click.prevent="toggle" size="tiny">show source code</Button>
  </div>
</template>

<script setup>
const sourceCodeMap = useRuntimeConfig().public.sourceCodeMap;
const route = useRoute();
const sourceCode = computed(() => sourceCodeMap[route.path + ".vue"] || "");
const show = ref(false);

function toggle() {
  show.value = !show.value;
}
</script>
