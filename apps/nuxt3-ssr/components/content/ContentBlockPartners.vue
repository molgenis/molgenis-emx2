<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  partners: IPartner[];
}>();
const linkToWebsite = (partner: IPartner) => {
  if (partner?.website) {
    const url = partner?.website;
    const protocolCheck = new RegExp("^https?:\/\/");
    if (!protocolCheck.test(url)) return `https://${url}`;
    else return url;
  } else {
    return undefined;
  }
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
        :links="[{ title: 'Read more', url: linkToWebsite(partner) }]" />
    </ReferenceCardList>
  </ContentBlock>
</template>
