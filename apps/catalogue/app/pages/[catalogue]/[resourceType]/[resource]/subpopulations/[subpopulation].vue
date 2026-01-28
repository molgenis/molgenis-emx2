<script setup lang="ts">
import { useRuntimeConfig, useRoute, useFetch, useHead } from "#app";
import { buildTree } from "#imports";
import { moduleToString } from "../../../../../../../tailwind-components/app/utils/moduleToString";
import { computed, reactive } from "vue";
import type { ISubpopulations } from "../../../../../../interfaces/catalogue";
import type { IMgError } from "../../../../../../interfaces/types";
import dateUtils from "../../../../../utils/dateUtils";
import subpopulationGql from "../../../../../gql/subpopulation";
import { removeChildIfParentSelected } from "../../../../../utils/treeHelpers";
import LayoutsDetailPage from "../../../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import SideNavigation from "../../../../../components/SideNavigation.vue";
import ContentBlocks from "../../../../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import ContentBlock from "../../../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import ContentOntology from "../../../../../../../tailwind-components/app/components/content/Ontology.vue";
import CatalogueItemList from "../../../../../components/CatalogueItemList.vue";
import type { Crumb } from "../../../../../../../tailwind-components/types/types";
const config = useRuntimeConfig();
const route = useRoute();
const schema = config.public.schema as string;
const query = moduleToString(subpopulationGql);

interface ISubpopulationQueryResponse {
  data: {
    Subpopulations: ISubpopulations[];
  };
}

const { data } = await useFetch<ISubpopulationQueryResponse, IMgError>(
  `/${schema}/graphql`,
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
);

if (data.value?.data?.Subpopulations?.length === 0) {
  throw new Error("Subpopulation not found");
}

const subpopulation = data.value?.data?.Subpopulations?.[0] as ISubpopulations;
useHead({
  title: subpopulation.name,
  meta: [{ name: "description", content: subpopulation.description }],
});

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const pageCrumbs: Crumb[] = [
  {
    label: cohortOnly.value ? "home" : (route.params.catalogue as string),
    url: `/${route.params.catalogue}`,
  },

  {
    label: route.params.resourceType as string,
    url: `/${route.params.catalogue}/${route.params.resourceType}`,
  },

  {
    label: route.params.resource as string,
    url: `/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}`,
  },

  {
    label: "Subpopulations",
    url: `/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}/subpopulations`,
  },

  {
    label: route.params.subpopulation as string,
    url: `/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}/subpopulations/${route.params.subpopulation}`,
  },
];

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

if (subpopulation.numberOfParticipants) {
  items.push({
    label: "Number of participants",
    content: subpopulation.numberOfParticipants,
  });
}
if (subpopulation.inclusionStart || subpopulation.inclusionEnd) {
  items.push({
    label: "Start/end year",
    content: dateUtils.startEndYear(
      subpopulation.inclusionStart,
      subpopulation.inclusionEnd
    ),
  });
}

if (subpopulation.countries) {
  items.push({
    label: "Countries",
    content: renderList(
      subpopulation.countries.sort((a, b) => (b.order ?? 0) - (a.order ?? 0)),
      toName
    ),
  });
}

if (subpopulation.inclusionCriteria) {
  items.push({
    label: "Inclusion criteria",
    content: renderList(
      subpopulation.inclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation.otherInclusionCriteria) {
  items.push({
    label: "Other inclusion criteria",
    content: subpopulation.otherInclusionCriteria,
  });
}

if (subpopulation.exclusionCriteria) {
  items.push({
    label: "Exclusion criteria",
    content: renderList(
      subpopulation.exclusionCriteria.sort(
        (a, b) => (b.order ?? 0) - (a.order ?? 0)
      ),
      toName
    ),
  });
}

if (subpopulation.otherExclusionCriteria) {
  items.push({
    label: "Other exclusion criteria",
    content: subpopulation.otherExclusionCriteria,
  });
}

if (subpopulation.ageGroups?.length) {
  tocItems.push({ label: "Age categories", id: "age_categories" });
}

const mainMedicalConditionTree = computed(() => {
  if (subpopulation.mainMedicalCondition?.length) {
    return buildTree(subpopulation.mainMedicalCondition);
  } else {
    return [];
  }
});

const comorbidityTree = computed(() => {
  if (subpopulation.comorbidity?.length) {
    return buildTree(subpopulation.comorbidity);
  } else {
    return [];
  }
});

if (subpopulation.mainMedicalCondition?.length) {
  tocItems.push({
    label: "Main medical condition",
    id: "main_medical_condition",
  });
}
if (subpopulation.comorbidity?.length) {
  tocItems.push({ label: "Comorbidity", id: "comorbidity" });
}
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="page-header"
        :title="subpopulation.name"
        :description="subpopulation.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="subpopulation.name"
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
    </template>
  </LayoutsDetailPage>
</template>
