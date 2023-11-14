<script setup lang="ts">
const config = useRuntimeConfig();
</script>
<template>
  <LandingCohortsOnly v-if="config.public.cohortOnly" />
  <LandingCentral />
</template>
