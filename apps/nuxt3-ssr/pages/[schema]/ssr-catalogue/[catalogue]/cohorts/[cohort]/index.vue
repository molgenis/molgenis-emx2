<script setup lang="ts">
const route = useRoute();
</script>
<template>
  <CohortDetails :catalogue="route.params.catalogue" />
</template>
