<script setup lang="ts">
import { INetwork } from "~/interfaces/types";

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
        :url="network.website"
        :links="[{ title: 'Read more', url: network.website }]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
