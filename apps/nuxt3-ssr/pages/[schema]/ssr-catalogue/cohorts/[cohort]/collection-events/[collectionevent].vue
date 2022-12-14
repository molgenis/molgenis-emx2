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

let tocItems = reactive([
  { label: "Details", id: "details" },
]);


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

let ageGroupsTree = [];
if (collectionEvent?.ageGroups?.length) {
  ageGroupsTree = buildOntologyTree(collectionEvent.ageGroups)
  tocItems.push({ label: "Age categories", id: "age_categories" })
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

let dataCategoriesTree = [];
if (collectionEvent?.dataCategories?.length) {
  dataCategoriesTree = buildOntologyTree(collectionEvent.dataCategories)
  tocItems.push({ label: "Data categories", id: "data_catagories" })
}

let areasOfInformationTree = [];
if (collectionEvent?.areasOfInformation?.length) {
  areasOfInformationTree = buildOntologyTree(collectionEvent.areasOfInformation)
  tocItems.push({ label: "Areas of information", id: "areas_of_information" })
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
      <PageHeader :title="collectionEvent?.name" :description="collectionEvent?.description">
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="collectionEvent?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="collectionEvent">
        <ContentBlock id="details" title="Details">
          <DefinitionList :items="items" :collapse-all="false" />
        </ContentBlock>
        <ContentBlock id="age_categories" title="Age categories">
          <ContentOntology :tree="ageGroupsTree" :collapse-all="false" />
        </ContentBlock>
        <ContentBlock id="data_catagories" title="Data catagories">
          <ContentOntology :tree="dataCategoriesTree" :collapse-all="false" />
        </ContentBlock>
        <ContentBlock id="areas_of_information" title="Areas of information">
          <ContentOntology :tree="areasOfInformationTree" :collapse-all="false" />
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
