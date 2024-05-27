<script setup lang="ts">
const { schema, table } = useRoute().params;
</script>
<template>
  <div>
    <h1>{{ schema }}</h1>
    <h2>{{ table }}</h2>
  </div>
</template>
