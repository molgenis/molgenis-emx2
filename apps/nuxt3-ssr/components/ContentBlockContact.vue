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
        v-for="contact in contributors"
        :contact="contact?.contact"
      />
    </ContactCardList>
  </ContentBlock>
</template>
