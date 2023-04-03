<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  contributors: IContributor[];
}>();
</script>

<template>
  <ContentBlock :title="title">
    <ContactCardList>
      <ContactCard v-for="contributor in contributors" :contact="contributor" />
    </ContactCardList>
  </ContentBlock>
</template>
