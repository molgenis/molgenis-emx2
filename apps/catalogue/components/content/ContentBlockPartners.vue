<script setup lang="ts">
import type { IPartner } from "~/interfaces/types";

defineProps<{
  title: string;
  description?: string;
  partners: IPartner[];
}>();
const linkToWebsite = (partner: IPartner) => {
  const url = partner?.website;
  const protocolCheck = new RegExp("^https?:\/\/");
  if (!protocolCheck.test(url)) return `https://${url}`;
  else return url;
};
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ReferenceCardList>
      <ReferenceCard
        v-for="partner in partners"
        :imageUrl="partner?.logo?.url"
        :title="partner?.name"
        :description="partner?.description"
        :url="linkToWebsite(partner)"
        :links="[{ title: 'Read more', url: linkToWebsite(partner) }]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
