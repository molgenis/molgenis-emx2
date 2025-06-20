<script setup lang="ts">
import dateUtils from "../../../../../utils/dateUtils";
import collectionEventGql from "../../../../../gql/collectionEvent";
import type {
  IDefinitionListItem,
  IMgError,
  IOntologyItem,
} from "../../../../../interfaces/types";
import { useRuntimeConfig, useRoute, useFetch, useHead } from "#app";
import { moduleToString, logError, buildTree } from "#imports";
import { computed, reactive } from "vue";
import { removeChildIfParentSelected } from "../../../../../utils/treeHelpers";

const config = useRuntimeConfig();
const schema = config.public.schema as string;
const route = useRoute();

const query = moduleToString(collectionEventGql);

const { data, error } = await useFetch<any, IMgError>(`/${schema}/graphql`, {
  method: "POST",
  body: {
    query,
    variables: {
      id: route.params.resource,
      name: route.params.collectionevent,
    },
  },
});

if (error.value) {
  const contextMsg = "Error fetching data for collection-event detail page";
  logError(error.value, contextMsg);
  throw new Error(contextMsg);
}

const collectionEvent: any = computed(
  () => data.value.data.CollectionEvents[0]
);

let tocItems = reactive([{ label: "Details", id: "details" }]);

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const pageCrumbs: any = {};

pageCrumbs[
  cohortOnly.value ? "home" : (route.params.catalogue as string)
] = `/${route.params.catalogue}`;

pageCrumbs[
  route.params.resourceType as string
] = `/${route.params.catalogue}/${route.params.resourceType}`;

pageCrumbs[
  route.params.resource as string
] = `/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}`;

pageCrumbs["Collection events"] = "";
pageCrumbs[route.params.collectionevent as string] = "";

function renderList(list: any[], itemMapper: (a: any) => string) {
  return list?.length === 1 ? itemMapper(list[0]) : list.map(itemMapper);
}

const toName = (item: any) => item.name;

const items: IDefinitionListItem[] = [];
if (collectionEvent.value?.cohorts?.length) {
  items.push({
    label: "Cohorts",
    content: renderList(collectionEvent.value?.cohorts, toName),
  });
}

if (collectionEvent.value?.startYear || collectionEvent.value?.endYear) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(
      collectionEvent.value.startYear && collectionEvent.value.startYear.name
        ? collectionEvent.value.startYear.name
        : null,
      collectionEvent.value.endYear && collectionEvent.value.endYear.name
        ? collectionEvent.value.endYear.name
        : null
    ),
  });
}

if (collectionEvent.value.sampleCategories?.length) {
  tocItems.push({ label: "Sample categories", id: "sample_categories" });
}

if (collectionEvent.value?.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

if (collectionEvent.value?.coreVariables?.length) {
  tocItems.push({ label: "Core variables", id: "core_variables" });
}

if (collectionEvent.value?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: collectionEvent.value?.numberOfParticipants,
  });
}

let dataCategoriesTree: IOntologyItem[] = [];
if (collectionEvent.value?.dataCategories?.length) {
  dataCategoriesTree = buildTree(collectionEvent.value.dataCategories);
  tocItems.push({ label: "Data categories", id: "data_categories" });
}

let areasOfInformationTree: IOntologyItem[] = [];
if (collectionEvent.value?.areasOfInformation?.length) {
  areasOfInformationTree = buildTree(collectionEvent.value.areasOfInformation);
  tocItems.push({ label: "Areas of information", id: "areas_of_information" });
}

let standardizedToolsTree: IOntologyItem[] = [];
if (collectionEvent.value.standardizedTools) {
  standardizedToolsTree = buildTree(collectionEvent.value.standardizedTools);
  tocItems.push({ label: "Standardized tools", id: "standardized_tools" });
}

useHead({
  title: collectionEvent.value?.name,
  meta: [{ name: "description", content: collectionEvent.value?.description }],
});
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="page-header"
        :title="collectionEvent?.name"
        :description="collectionEvent?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="collectionEvent?.name"
        :items="tocItems"
        header-target="#page-header"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="collectionEvent">
        <ContentBlock v-if="collectionEvent" id="details" title="Details">
          <CatalogueItemList :items="items" :collapse-all="false" />
        </ContentBlock>

        <ContentBlock
          v-if="collectionEvent.sampleCategories"
          id="sample_categories"
          title="Sample categories"
        >
          <ContentOntology
            :tree="buildTree(collectionEvent.sampleCategories)"
            :collapse-all="false"
          />
        </ContentBlock>

        <ContentBlock
          v-if="collectionEvent?.coreVariables"
          id="core_variables"
          title="Core variables"
        >
          <ul class="grid gap-1 pl-4 list-disc list-outside">
            <li v-for="coreVariable in collectionEvent.coreVariables.sort()">
              {{ coreVariable }}
            </li>
          </ul>
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
