<script setup lang="ts">
import type { IOrganisations } from "../../interfaces/catalogue";
import ContentBlockModal from "../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import CatalogueItemList from "./CatalogueItemList.vue";

const props = defineProps<{
  organisation: IOrganisations;
}>();
</script>
<template>
  <ContentBlockModal
    :title="props.organisation?.organisation?.name ?? ''"
    description="Lead organisation"
  >
    <CatalogueItemList
      :items="[
        {
          label: 'role',
          content: props.organisation?.role
            ? props.organisation?.role.map((c) => c.name).join(', ')
            : undefined,
        },
        {
          label: 'website',
          content: props.organisation?.organisationWebsite,
        },
        {
          label: 'acronym',
          content: props.organisation?.organisation?.acronym,
        },
        {
          label: 'country',
          content: props.organisation?.organisation?.country,
        },
        {
          label: 'logo',
          content: props.organisation?.logo,
        },
      ]"
    />
  </ContentBlockModal>
</template>
