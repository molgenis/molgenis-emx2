<script setup lang="ts">
import cohortGql from "~~/gql/cohort";

const props = defineProps<{
  id: string;
}>();

const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(cohortGql);

let cohort: ICohort = ref();

const { data: cohortData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: {
        id: props.id,
      },
    },
  }
).catch((e) => console.log(e));

watch(
  cohortData,
  (data) => {
    cohort = data?.data?.Cohorts[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

const items = [];

if (cohort?.website) {
  items.push({
    label: "Website",
    content: cohort.website,
  });
}

if (cohort?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: cohort?.numberOfParticipants,
  });
}

if (cohort?.numberOfParticipantsWithSamples) {
  items.push({
    label: "Number of participants with samples",
    content: cohort?.numberOfParticipantsWithSamples,
  });
}
</script>

<template>
  <ContentBlockModal
    :title="cohort?.name"
    :description="cohort?.description"
    v-if="cohort"
  >
    <CatalogueItemList :items="items" :small="true" />
  </ContentBlockModal>
</template>
