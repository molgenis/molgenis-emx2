<script setup lang="ts">
import subcohortGql from "~~/gql/subcohort";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(subcohortGql);

let subcohort: Ref = ref();
const { data: subcohortData } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query,
      variables: { id: route.params.cohort, name: route.params.subcohort },
    },
  }
).catch((e) => console.log(e));

watch(
  subcohortData,
  function setData(data: any) {
    subcohort = data?.data?.Subcohorts[0];
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
  "Cohorts"
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts`;

// @ts-ignore
pageCrumbs[
  route.params.cohort as string
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts/${route.params.cohort}`;

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

if (subcohort?.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subcohort.numberOfParticipants,
  });
}
if (subcohort?.inclusionStart || subcohort?.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: filters.startEndYear(
      subcohort.inclusionStart,
      subcohort.inclusionEnd
    ),
  });
}

if (subcohort?.countries) {
  items.push({
    label: "Population",
    content: renderList(
      subcohort.countries.sort((a, b) => b.order - a.order),
      toName
    ),
  });
}

if (subcohort?.inclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subcohort.inclusionCriteria,
  });
}

if (subcohort?.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

let mainMedicalConditionTree = [];
if (subcohort?.mainMedicalCondition?.length) {
  mainMedicalConditionTree = buildTree(subcohort.mainMedicalCondition);
  tocItems.push({
    label: "Main medical condition",
    id: "main_medical_condition",
  });
}

let comorbidityTree = [];
if (subcohort?.comorbidity?.length) {
  comorbidityTree = buildTree(subcohort.comorbidity);
  tocItems.push({ label: "Comorbidity", id: "comorbidity" });
}

// todo add count table ( empty in current test set)

useHead({ title: subcohort?.name });
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="subcohort?.name"
        :description="subcohort?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="subcohort?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="subcohort">
        <ContentBlock id="details" title="Details">
          <CatalogueItemList :items="items" />
        </ContentBlock>
        <ContentBlock
          v-if="subcohort.ageGroups"
          id="age_categories"
          title="Age categories"
        >
          <ul class="grid gap-1 pl-4 list-disc list-outside">
            <li
              v-for="ageGroup in removeChildIfParentSelected(
                subcohort.ageGroups || []
              ).sort((a, b) => a.order - b.order)"
              :key="ageGroup.name"
            >
              {{ ageGroup.name }}
            </li>
          </ul>
        </ContentBlock>
        <ContentBlock
          v-if="subcohort.mainMedicalCondition"
          id="main_medical_condition"
          title="Main medical condition"
        >
          <ContentOntology
            :tree="mainMedicalConditionTree"
            :collapse-all="false"
          />
        </ContentBlock>
        <ContentBlock
          v-if="subcohort.comorbidity"
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
