<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();
</script>
<template>
  <LandingCohortsOnly v-if="config.public.cohortOnly" />
  <LandingCentral v-else :schema="route.params.schema" :baseURL="route.path" />
</template>
