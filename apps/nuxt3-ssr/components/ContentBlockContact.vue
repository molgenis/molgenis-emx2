<script setup>
defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
  contributors: {
    type: Array,
  },
});
</script>

<template>
  <ContentBlock :title="title">
    <ContactCardList>
      <ContactCard
        v-for="contributor in contributors"
        :contact="contributor?.contact"
        :contributionDescription="contributor?.contributionDescription"
      />
    </ContactCardList>
  </ContentBlock>
</template>
