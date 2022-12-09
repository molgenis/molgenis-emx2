<script setup lang="ts">
import { Ref } from "vue";
import query from "~~/gql/collectionEvent";
const config = useRuntimeConfig();
const route = useRoute();

if (query.loc?.source.body === undefined) {
  throw "unable to load query: " + query.toString();
}
const queryValue = query.loc?.source.body;

let collectionEvent: Ref = ref();
const { data: collectionEventData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: queryValue,
      variables: {
        pid: route.params.cohort,
        name: route.params.collectionevent,
      },
    },
  }
).catch((e) => console.log(e));

watch(
  collectionEventData,
  function setData(data: any) {
    collectionEvent = data?.data?.CollectionEvents[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

const pageCrumbs: any = {
  Cohorts: `/${route.params.schema}/ssr-catalogue`,
};

// @ts-ignore
pageCrumbs[
  route.params.cohort
] = `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}`;

function renderList(list: any[], itemMapper: (a: any) => string) {
  return list?.length === 1 ? itemMapper(list[0]) : list.map(itemMapper);
}

const toName = (item: any) => item.name;

const items = [];
if (collectionEvent?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collectionEvent?.numberOfParticipants,
  });
}
if (collectionEvent?.subcohorts?.length) {
  items.push({
    label: "Subcohorts",
    content: renderList(collectionEvent?.subcohorts, toName),
  });
}
if (collectionEvent?.numberOfParticipants?.length) {
  items.push({
    label: "Number of participants",
    value: renderList(collectionEvent?.numberOfParticipants, toName),
  });
}
if (collectionEvent?.ageGroups?.lenght) {
  items.push({
    label: "Age categories",
    content: renderList(collectionEvent?.ageGroups, toName),
  });
}

if (collectionEvent?.startYear || collectionEvent?.endYear) {
  items.push({
    label: "Start/end year: ",
    content: filters.startEndYear(
      collectionEvent.startYear && collectionEvent.startYear.name
        ? collectionEvent.startYear.name
        : null,
      collectionEvent.endYear && collectionEvent.endYear.name
        ? collectionEvent.endYear.name
        : null
    ),
  });
}

if (collectionEvent?.dataCategories?.length) {
  items.push({
    label: "Data Categories",
    content: renderList(collectionEvent?.dataCategories, toName),
  });
}

if (collectionEvent?.areasOfInformation?.length) {
  items.push({
    label: "Areas of information",
    content: renderList(collectionEvent?.areasOfInformation, toName),
  });
}

if (collectionEvent?.sampleCategories?.length) {
  items.push({
    label: "Sample categories",
    content: renderList(collectionEvent?.sampleCategories, toName),
  });
}

if (collectionEvent?.coreVariables?.length) {
  items.push({
    label: "Core variables",
    content: renderList(collectionEvent?.coreVariables, toName),
  });
}

items.sort((a, b) => a.label.localeCompare(b.label));
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="collectionEvent?.name"
        :description="collectionEvent?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <ContentBlocks v-if="collectionEvent">
        <ContentBlock title="Details">
          <DefinitionList :items="items" />
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
