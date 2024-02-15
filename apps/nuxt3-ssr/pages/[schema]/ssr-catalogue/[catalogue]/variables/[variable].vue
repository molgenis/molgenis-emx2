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
  ? { networks: { equals: [{ id: catalogueRouteParam }] } }
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
      const status = calcIndividualVariableHarmonizationStatus(variable.value, [
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
          id="harmonization-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <HarmonizationGridPerVariable
            v-if="isRepeating"
            :cohorts-with-mapping="cohortsWithMapping"
            :variable="variable"
          />
          <HarmonizationListPerVariable
            v-else
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
