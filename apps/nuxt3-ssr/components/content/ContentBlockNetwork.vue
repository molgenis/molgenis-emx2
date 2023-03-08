<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  networks: INetwork[];
}>();
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ReferenceCardList>
      <ReferenceCard
        v-for="network in networks"
        :imageUrl="network?.logo?.url"
        :title="network?.name"
        :description="network?.description"
        url="#"
        :links="[{ title: 'Read more', url: '#' }]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
