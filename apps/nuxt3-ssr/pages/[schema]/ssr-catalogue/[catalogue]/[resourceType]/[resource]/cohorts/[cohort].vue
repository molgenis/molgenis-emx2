<script setup lang="ts">
import dateUtils from "~/utils/dateUtils";
import cohortGql from "~~/gql/cohort";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(cohortGql);

let cohort: Ref = ref();
const { data: cohortData } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: {
    query,
    variables: { id: route.params.resource, name: route.params.cohor },
  },
}).catch((e) => console.log(e));

watch(
  cohortData,
  function setData(data: any) {
    cohort = data?.data?.ResourceCohorts[0];
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
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts/${route.params.resources}`;

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

if (cohort?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: cohort.numberOfParticipants,
  });
}
if (cohort?.inclusionStart || cohort?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(cohort.inclusionStart, cohort.inclusionEnd),
  });
}

if (cohort?.countries) {
  items.push({
    label: "Countries",
    content: renderList(
      cohort.countries.sort((a, b) => b.order - a.order),
      toName
    ),
  });
}

if (cohort?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: cohort.inclusionCriteria,
  });
}

if (cohort?.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

let mainMedicalConditionTree = [];
if (cohort?.mainMedicalCondition?.length) {
  mainMedicalConditionTree = buildTree(cohort.mainMedicalCondition);
  tocItems.push({
    label: "Main medical condition",
    id: "main_medical_condition",
  });
}

let comorbidityTree = [];
if (cohort?.comorbidity?.length) {
  comorbidityTree = buildTree(cohort.comorbidity);
  tocItems.push({ label: "Comorbidity", id: "comorbidity" });
}

// todo add count table ( empty in current test set)

useHead({ title: cohort?.name });
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="cohort?.name" :description="cohort?.description">
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="cohort?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="cohort">
        <ContentBlock id="details" title="Details">
          <CatalogueItemList :items="items" />
        </ContentBlock>
        <ContentBlock
          v-if="cohort.ageGroups"
          id="age_categories"
          title="Age categories"
        >
          <ul class="grid gap-1 pl-4 list-disc list-outside">
            <li
              v-for="ageGroup in removeChildIfParentSelected(
                cohort.ageGroups || []
              ).sort((a, b) => a.order - b.order)"
              :key="ageGroup.name"
            >
              {{ ageGroup.name }}
            </li>
          </ul>
        </ContentBlock>
        <ContentBlock
          v-if="cohort.mainMedicalCondition"
          id="main_medical_condition"
          title="Main medical condition"
        >
          <ContentOntology
            :tree="mainMedicalConditionTree"
            :collapse-all="false"
          />
        </ContentBlock>
        <ContentBlock
          v-if="cohort.comorbidity"
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
