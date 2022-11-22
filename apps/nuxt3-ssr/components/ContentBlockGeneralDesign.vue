<script setup>
defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
  definition: {
    type: Array,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <DefinitionList :items="definition" />
  </ContentBlock>
</template>
