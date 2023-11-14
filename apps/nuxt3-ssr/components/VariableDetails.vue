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

let tocItems = reactive([
  { label: "Description", id: "description" },
  { label: "Per cohort harmonization", id: "harmonization-per-cohort" },
  { label: "Harmonization details", id: "harmonization-details-per-cohort" },
]);

let crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    `${route.params.catalogue}`
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
  crumbs[
    "variables"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/variables`;
} else {
  crumbs = {
    Home: `/${route.params.schema}/ssr-catalogue`,
    Variables: `/${route.params.schema}/ssr-catalogue/all/variables`,
  };
}
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="variable?.name" :description="variable?.label">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" :current="variable.name" />
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
          id="harmonization-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <HarmonizationListPerVariable
            :variable="variable"
            :cohorts="cohorts"
          />
        </ContentBlock>

        <ContentBlock
          id="harmonization-details-per-cohort"
          title="Harmonization details per Cohort"
          description="Select a Cohort to see the details of the harmonization"
        >
          <HarmonizationVariableDetails
            :variable="variable"
            :cohorts="cohorts"
          />
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
