<script setup lang="ts">
import variableQuery from "~~/gql/variable";
import type { IVariable, IVariableMappings } from "~/interfaces/types";
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
  { label: "Harmonization status", id: "harmonization-per-cohort" },
]);
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
          <DefinitionList
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
          </DefinitionList>
        </ContentBlock>

        <ContentBlock
          id="harmonization-status-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <HarmonizationListPerVariable
            :variable="variable"
            :cohorts="cohorts"
          />
        </ContentBlock>

        <ContentBlock
          id="harmonization--details per-cohort"
          title="Harmonization details per Cohort"
          description="Explanation about available data and the functionality seen here."
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
