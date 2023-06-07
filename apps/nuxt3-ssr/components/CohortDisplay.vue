<script setup lang="ts">
import query from "~~/gql/cohort";

const props = defineProps<{
  id: string;
}>();

const config = useRuntimeConfig();
const route = useRoute();

if (query.loc?.source.body === undefined) {
  throw "unable to load query: " + query.toString();
}
const queryValue = query.loc?.source.body;

let cohort: ICohort = ref();

const { data: cohortData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: queryValue,
      variables: {
        id: props.id,
      },
    },
  }
).catch(e => console.log(e));

watch(
  cohortData,
  data => {
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
  <ContentBlock
    :title="cohort?.name"
    :description="cohort?.description"
    v-if="cohort">
    <DefinitionList :items="items" :small="true" />
  </ContentBlock>
</template>
