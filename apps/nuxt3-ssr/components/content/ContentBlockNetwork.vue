<script setup lang="ts">
const route = useRoute();
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
        :url="`/${route.params.schema}/ssr-catalogue/networks/${network.id}`"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
