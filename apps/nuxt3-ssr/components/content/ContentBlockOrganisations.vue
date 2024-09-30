<script setup lang="ts">
import type { IOrganisation } from "~/interfaces/types";

defineProps<{
  title: string;
  description?: string;
  organisations: IOrganisation[];
}>();
</script>

<template>
  <ContentBlock :title="title">
    <ContactCardList>
      <OrganisationCard
        v-for="organisation in organisations"
        :organisation="organisation"
      ></OrganisationCard>
    </ContactCardList>
  </ContentBlock>
</template>
