<script setup lang="ts">
import { Ref } from "vue";
import collectionEventGql from "~~/gql/collectionEvent";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(collectionEventGql);

let collectionEvent: Ref = ref();
const { data: collectionEventData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: {
        id: route.params.cohort,
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

let tocItems = reactive([{ label: "Details", id: "details" }]);

const pageCrumbs: any = {
  Home: `/${route.params.schema}/ssr-catalogue`,
  Cohorts: `/${route.params.schema}/ssr-catalogue/cohorts`,
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
if (collectionEvent?.subcohorts?.length) {
  items.push({
    label: "Subcohorts",
    content: renderList(collectionEvent?.subcohorts, toName),
  });
}

if (collectionEvent?.startYear || collectionEvent?.endYear) {
  items.push({
    label: "Start/end year",
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

if (collectionEvent?.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

if (collectionEvent?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collectionEvent?.numberOfParticipants,
  });
}

let dataCategoriesTree = [];
if (collectionEvent?.dataCategories?.length) {
  dataCategoriesTree = buildOntologyTree(collectionEvent.dataCategories);
  tocItems.push({ label: "Data categories", id: "data_categories" });
}

if (collectionEvent?.sampleCategories?.length) {
  items.push({
    label: "Sample categories",
    content: renderList(collectionEvent?.sampleCategories, toName),
  });
}

let areasOfInformationTree = [];
if (collectionEvent?.areasOfInformation?.length) {
  areasOfInformationTree = buildOntologyTree(
    collectionEvent.areasOfInformation
  );
  tocItems.push({ label: "Areas of information", id: "areas_of_information" });
}

let standardizedToolsTree = [];
if (collectionEvent.standardizedTools) {
  standardizedToolsTree = buildOntologyTree(collectionEvent.standardizedTools);
  tocItems.push({ label: "Standardized tools", id: "standardized_tools" });
}

if (collectionEvent?.coreVariables?.length) {
  items.push({
    label: "Core variables",
    content: collectionEvent?.coreVariables,
  });
}

useHead({ title: collectionEvent?.name });
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
    <template #side>
      <SideNavigation :title="collectionEvent?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="collectionEvent">
        <ContentBlock v-if="collectionEvent" id="details" title="Details">
          <CatalogueItemList :items="items" :collapse-all="false" />
        </ContentBlock>
        <ContentBlock
          v-if="collectionEvent.ageGroups"
          id="age_categories"
          title="Age categories"
        >
          <ul class="grid gap-1 pl-4 list-disc list-outside">
            <li
              v-for="ageGroup in removeChildIfParentSelected(
                collectionEvent.ageGroups || []
              ).sort((a, b) => a.order - b.order)"
              :key="ageGroup.name"
            >
              {{ ageGroup.name }}
            </li>
          </ul>
        </ContentBlock>
        <ContentBlock
          v-if="collectionEvent.dataCategories"
          id="data_categories"
          title="Data categories"
        >
          <ContentOntology :tree="dataCategoriesTree" :collapse-all="false" />
        </ContentBlock>
        <ContentBlock
          v-if="collectionEvent.areasOfInformation"
          id="areas_of_information"
          title="Areas of information"
        >
          <ContentOntology
            :tree="areasOfInformationTree"
            :collapse-all="false"
          />
        </ContentBlock>
        <ContentBlock
          v-if="collectionEvent.standardizedTools"
          id="standardized_tools"
          title="Standardized tools"
        >
          <ContentOntology
            :tree="standardizedToolsTree"
            :collapse-all="false"
          />
          <CatalogueItemList
            v-if="collectionEvent.standardizedToolsOther"
            class="mt-6"
            :items="[
              {
                label: 'Standardized tools other',
                content: collectionEvent.standardizedToolsOther,
              },
            ]"
            :collapse-all="false"
          />
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
