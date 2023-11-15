<script setup lang="ts">
import variableQuery from "~~/gql/variable";
import type { IVariable, IVariableMappings } from "~/interfaces/types";
import { buildFilterFromKeysObject } from "meta-data-utils";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(variableQuery);
const { key } = useQueryParams();
const filter = buildFilterFromKeysObject(key);

let variable: VariableDetailsWithMapping;
let cohorts: { id: string }[];

type VariableDetailsWithMapping = IVariable &
  IVariableMappings & { nRepeats: number };

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables: { filter } },
  }
);

watch(data, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  variable = data?.data?.Variables[0] as VariableDetailsWithMapping;
  variable.nRepeats = data?.data?.RepeatedVariables_agg.count;
  cohorts = data?.data?.Cohorts;
}

const cohortsWithMapping = computed(() => {
  return cohorts
    .map((cohort) => {
      const status = calcHarmonizationStatus([variable], [cohort])[0][0];
      return {
        cohort,
        status,
      };
    })
    .filter(({ status }) => status !== "unmapped");
});

let tocItems = reactive([{ label: "Description", id: "description" }]);

if (cohortsWithMapping.value.length > 0) {
  tocItems.push({
    label: "Harmonization status per Cohort",
    id: "harmonization-per-cohort",
  });
  tocItems.push({
    label: "Harmonization details per Cohort",
    id: "harmonization-details-per-cohort",
  });
} else {
  tocItems.push({
    label: "Harmonization",
    id: "harmonization-details-no-mapping",
  });
}
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="variable?.name" :description="variable?.label">
        <template #prefix>
          <BreadCrumbs
            :crumbs="{
              Home: `/${route.params.schema}/ssr-catalogue`,
              Variables: `/${route.params.schema}/ssr-catalogue/variables`,
            }"
          />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="variable?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="variable">
        <ContentBlock
          id="description"
          title="Description"
          :description="variable?.description"
        >
          <CatalogueItemList
            :items="[
              {
                label: 'Unit',
                content: variable?.unit?.name,
              },
              {
                label: 'Format',
                content: variable?.format?.name,
              },
              {
                label: 'N repeats',
                content: variable?.nRepeats > 0 ? variable?.nRepeats : 'None',
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length > 0"
          id="harmonization-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <HarmonizationListPerVariable
            :cohortsWithMapping="cohortsWithMapping"
          />
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length > 0"
          id="harmonization-details-per-cohort"
          title="Harmonization details per Cohort"
          description="Select a Cohort to see the details of the harmonization"
        >
          <HarmonizationVariableDetails
            :variable="variable"
            :cohortsWithMapping="cohortsWithMapping"
          />
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length === 0"
          id="harmonization-details-no-mapping"
          title="Harmonization"
          description="No mapping found for this variable"
        >
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
