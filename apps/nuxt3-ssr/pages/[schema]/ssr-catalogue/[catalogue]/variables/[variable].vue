<script setup lang="ts">
import variableQuery from "~~/gql/variable";
import type { IVariable, IVariableMappings } from "~/interfaces/types";
import { buildFilterFromKeysObject } from "meta-data-utils";
const config = useRuntimeConfig();
const route = useRoute();

const query = moduleToString(variableQuery);
const scoped = route.params.catalogue !== "all";
const catalogueRouteParam = route.params.catalogue as string;
const { key } = useQueryParams();
const variableFilter = buildFilterFromKeysObject(key);
const cohortsFilter = scoped
  ? {
      _or: [
        { networks: { equals: [{ id: catalogueRouteParam }] } },
        {
          networks: { partOfNetworks: { id: { equals: catalogueRouteParam } } },
        },
      ],
    }
  : {};

type VariableDetailsWithMapping = IVariable &
  IVariableMappings & { nRepeats: number };

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: { query, variables: { variableFilter, cohortsFilter } },
  }
);

const variable = computed(
  () => data.value.data.Variables[0] as VariableDetailsWithMapping
);
const nRepeats = computed(() => data.value.data.RepeatedVariables_agg.count);
const cohorts = computed(() => data.value.data.Cohorts as { id: string }[]);
const isRepeating = computed(() => variable.value.repeats);

let crumbs: any = {};
crumbs[
  `${route.params.catalogue}`
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
crumbs[
  "variables"
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/variables`;

const cohortsWithMapping = computed(() => {
  return cohorts.value
    .map((cohort) => {
      const status = calcIndividualVariableHarmonisationStatus(variable.value, [
        cohort,
      ])[0];
      return {
        cohort,
        status,
      };
    })
    .filter(({ status }) =>
      Array.isArray(status)
        ? status.filter((s) => s !== "unmapped").length
        : status !== "unmapped"
    );
});

let tocItems = reactive([{ label: "Description", id: "description" }]);

if (cohortsWithMapping.value.length > 0) {
  tocItems.push({
    label: "Harmonisation status per Cohort",
    id: "harmonisation-per-cohort",
  });
  tocItems.push({
    label: "Harmonisation details per Cohort",
    id: "harmonisation-details-per-cohort",
  });
} else {
  tocItems.push({
    label: "Harmonisation",
    id: "harmonisation-details-no-mapping",
  });
}

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({ title: titlePrefix + variable.value.name });
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="variable?.name" :description="variable?.label">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
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
                content: nRepeats > 0 ? nRepeats : 'None',
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length > 0"
          id="harmonisation-per-cohort"
          title="Harmonisation status per Cohort"
          description="Overview of the harmonisation status per Cohort"
        >
          <HarmonisationGridPerVariable
            v-if="isRepeating"
            :cohorts-with-mapping="cohortsWithMapping"
            :variable="variable"
          />
          <HarmonisationListPerVariable
            v-else
            :cohortsWithMapping="cohortsWithMapping"
          />
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length > 0"
          id="harmonisation-details-per-cohort"
          title="Harmonisation details per Cohort"
          description="Select a Cohort to see the details of the harmonisation"
        >
          <HarmonisationVariableDetails
            :variable="variable"
            :cohortsWithMapping="cohortsWithMapping"
          />
        </ContentBlock>

        <ContentBlock
          v-if="cohortsWithMapping.length === 0"
          id="harmonisation-details-no-mapping"
          title="Harmonisation"
          description="No mapping found for this variable"
        >
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
