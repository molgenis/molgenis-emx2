<script setup>
defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
  partners: {
    type: Array,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ReferenceCardList>
      <ReferenceCard
        v-for="partner in partners"
        :imageUrl="partner?.institution?.logo?.url"
        :title="partner?.institution?.name"
        :description="partner?.institution?.description"
        url="#"
        :links="[{ title: 'Read more', url: '#' }]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
