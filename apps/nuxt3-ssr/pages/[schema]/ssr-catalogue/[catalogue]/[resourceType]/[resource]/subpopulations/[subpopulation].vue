<script setup lang="ts">
import dateUtils from "~/utils/dateUtils";
import subpopulationGql from "~~/gql/subpopulations";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(subpopulationGql);

let subpopulation: Ref = ref();
const { data: subpopulationData } = await useFetch(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: {
      query,
      variables: {
        id: route.params.resource,
        name: route.params.subpopulation,
      },
    },
  }
).catch((e) => console.log(e));

watch(
  subpopulationDataData,
  function setData(data: any) {
    subpopulation = data?.data?.Subpopulations[0];
  },
  {
    deep: true,
    immediate: true,
  }
);

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});
const pageCrumbs: any = {};
pageCrumbs[
  cohortOnly.value ? "home" : (route.params.catalogue as string)
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
pageCrumbs[
  "Resources"
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/resources`;

// @ts-ignore
pageCrumbs[
  route.params.collection as string
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/resources/${route.params.resources}`;

function renderList(
  list: any[],
  itemMapper: (a: any) => string,
  itemJoiner?: (a: any) => string
) {
  if (list?.length === 1) {
    return itemMapper(list[0]);
  }

  const mapped = list.map(itemMapper);
  if (!itemJoiner) {
    return mapped;
  }

  return itemJoiner(mapped);
}

const toName = (item: any) => item.name;

let tocItems = reactive([{ label: "Details", id: "details" }]);

const items: any = [];

if (subpopulation?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subpopulation.numberOfParticipants,
  });
}
if (subpopulation?.inclusionStart || subpopulation?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(
      subpopulation.inclusionStart,
      subpopulation.inclusionEnd
    ),
  });
}

if (subpopulation?.countries) {
  items.push({
    label: "Countries",
    content: renderList(
      subpopulation.countries.sort((a, b) => b.order - a.order),
      toName
    ),
  });
}

if (subpopulation?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subpopulation.inclusionCriteria,
  });
}

if (subpopulation?.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

let mainMedicalConditionTree = [];
if (subpopulation?.mainMedicalCondition?.length) {
  mainMedicalConditionTree = buildTree(subpopulation.mainMedicalCondition);
  tocItems.push({
    label: "Main medical condition",
    id: "main_medical_condition",
  });
}

let comorbidityTree = [];
if (subpopulation?.comorbidity?.length) {
  comorbidityTree = buildTree(subpopulation.comorbidity);
  tocItems.push({ label: "Comorbidity", id: "comorbidity" });
}

// todo add count table ( empty in current test set)

useHead({ title: subpopulation?.name });
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="page-header"
        :title="subpopulation?.name"
        :description="subpopulation?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="subpopulation?.name"
        :items="tocItems"
        header-target="#page-header"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="subpopulation">
        <ContentBlock id="details" title="Details">
          <CatalogueItemList :items="items" />
        </ContentBlock>
        <ContentBlock
          v-if="subpopulation.ageGroups"
          id="age_categories"
          title="Age categories"
        >
          <ul class="grid gap-1 pl-4 list-disc list-outside">
            <li
              v-for="ageGroup in removeChildIfParentSelected(
                subpopulation.ageGroups || []
              ).sort((a, b) => a.order - b.order)"
              :key="ageGroup.name"
            >
              {{ ageGroup.name }}
            </li>
          </ul>
        </ContentBlock>
        <ContentBlock
          v-if="subpopulation.mainMedicalCondition"
          id="main_medical_condition"
          title="Main medical condition"
        >
          <ContentOntology
            :tree="mainMedicalConditionTree"
            :collapse-all="false"
          />
        </ContentBlock>
        <ContentBlock
          v-if="subpopulation.comorbidity"
          id="comorbidity"
          title="Comorbidity"
        >
          <ContentOntology :tree="comorbidityTree" :collapse-all="false" />
        </ContentBlock>
      </ContentBlocks>
      <!-- todo add count table ( empty in current test set) -->
    </template>
  </LayoutsDetailPage>
</template>
