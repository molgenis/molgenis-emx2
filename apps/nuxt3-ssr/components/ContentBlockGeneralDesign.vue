<script setup>
import ContentBlock from "./ContentBlock.vue";
import DefinitionList from "./DefinitionList.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <DefinitionList />
  </ContentBlock>
</template>
