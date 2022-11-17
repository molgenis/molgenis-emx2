<script setup>
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
    <ReferenceCardList>
      <ReferenceCard
        imageUrl="https://via.placeholder.com/137x150"
        title="University Medical Centre Groningen"
        description="The University Medical Center Groningen (UMCG) is one of the
              largest hospitals in the Netherlands and the largest employer in
              the Northern Netherlands. The more than 12,000 employees work
              together on care, research, training and education with the common
              goal: building the future of health."
        url="#"
        :links="[{ title: 'Read more', url: '#' }]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
