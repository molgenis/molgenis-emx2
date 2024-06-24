<script setup lang="ts">
import type { IOrganization } from "~/interfaces/types";
const props = defineProps<{
  organisation: IOrganization;
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
          label: 'email',
          content: props.organisation?.email,
        },
        {
          label: 'description',
          content: props.organisation?.description,
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
          label: 'type',
          content: props.organisation?.type?.name,
        },
        {
          label: 'institution',
          content: props.organisation?.institution,
        },
        {
          label: 'institutionAcronym',
          content: props.organisation?.institutionAcronym,
        },
        {
          label: 'typeOther',
          content: props.organisation?.typeOther,
        },
        {
          label: 'address',
          content: props.organisation?.address,
        },
        {
          label: 'expertise',
          content: props.organisation?.expertise,
        },
        {
          label: 'country',
          content: props.organisation?.country?.name,
        },
        {
          label: 'logo',
          content: props.organisation?.logo,
        },
      ]"
    />
  </ContentBlockModal>
</template>
