<script setup lang="ts">
import type { IOrganisations } from "../../../interfaces/catalogue";
import ContactCardList from "../ContactCardList.vue";
import OrganisationCard from "../OrganisationCard.vue";
import DefinitionListTerm from "../../../../tailwind-components/app/components/DefinitionListTerm.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";

defineProps<{
  title: string;
  description?: string;
  organisations: IOrganisations[];
}>();
</script>

<template>
  <ContentBlock :title="title">
    <template v-if="organisations.find((o) => o.isLeadOrganisation)">
      <DefinitionListTerm>
        <div class="flex items-center gap-1">Lead organisations</div>
      </DefinitionListTerm>
      <ContactCardList>
        <OrganisationCard
          v-for="organisation in organisations.filter(
            (o) => o.isLeadOrganisation
          )"
          :organisation="organisation"
        ></OrganisationCard>
      </ContactCardList>
    </template>
    <template v-if="organisations.find((o) => !o.isLeadOrganisation)">
      <DefinitionListTerm class="mt-11">
        <div class="flex items-center gap-1">Additional organisations</div>
      </DefinitionListTerm>
      <ContactCardList>
        <OrganisationCard
          v-for="organisation in organisations.filter(
            (o) => !o.isLeadOrganisation
          )"
          :organisation="organisation"
        ></OrganisationCard>
      </ContactCardList>
    </template>
  </ContentBlock>
</template>
