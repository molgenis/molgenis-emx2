<script setup lang="ts">
import type { IOrganisations } from "../interfaces/catalogue";
const props = defineProps<{
  organisation: IOrganisations;
}>();
</script>
<template>
  <ContentBlockModal
    :title="props.organisation.name ?? ''"
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
          content: props.organisation?.website,
        },
        {
          label: 'acronym',
          content: props.organisation?.acronym,
        },
        {
          label: 'country',
          content: props.organisation?.country
            ? props.organisation?.country.map((c) => c.name).join(', ')
            : undefined,
        },
        {
          label: 'logo',
          content: props.organisation?.logo,
        },
      ]"
    />
  </ContentBlockModal>
</template>
