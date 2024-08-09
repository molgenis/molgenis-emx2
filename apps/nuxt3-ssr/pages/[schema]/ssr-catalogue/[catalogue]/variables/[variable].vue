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
const collectionFilter = scoped
  ? {
      _or: [
        { collections: { equals: [{ id: catalogueRouteParam }] } },
        {
          collections: {
            partOfCollections: { id: { equals: catalogueRouteParam } },
          },
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
    body: { query, variables: { variableFilter, collectionFilter } },
  }
);

const variable = computed(
  () => data.value.data.Variables[0] as VariableDetailsWithMapping
);
const collections = computed(
  () => data.value.data.Collections as { id: string }[]
);
const isRepeating = computed(() => variable.value.repeatUnit?.name);

let crumbs: any = {};
crumbs[
  `${route.params.catalogue}`
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
crumbs[
  "variables"
] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/variables`;

const collectionsWithMapping = computed(() => {
  if (!collections.value) return [];
  return collections.value
    .map((collection) => {
      const status = calcIndividualVariableHarmonisationStatus(variable.value, [
        collection,
      ])[0];
      return {
        collection,
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

if (collectionsWithMapping.value.length > 0) {
  tocItems.push({
    label: "Harmonisation status per data source",
    id: "harmonisation-per-collection",
  });
  tocItems.push({
    label: "Harmonisation details per data source",
    id: "harmonisation-details-per-collection",
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
        <ContentBlock id="definition" title="Definition">
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
                label: 'Repeated for',
                content: variable?.repeatUnit?.name
                  ? variable?.repeatUnit?.name +
                    ' ' +
                    variable?.repeatMin +
                    '-' +
                    variable?.repeatMax
                  : undefined,
              },
              {
                label: 'Description',
                content: variable?.description,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock
          v-if="collectionsWithMapping.length > 0"
          id="harmonisation-per-collection"
          title="Harmonisation status per Data source"
        >
          <HarmonisationGridPerVariable
            v-if="isRepeating"
            :variable="variable"
          />
          <HarmonisationListPerVariable v-else :mappings="variable.mappings" />
        </ContentBlock>

        <ContentBlock
          v-if="collectionsWithMapping.length > 0"
          id="harmonisation-details-per-cohort"
          title="Harmonisation details per Collection"
          description="Select a Cohort to see the details of the harmonisation"
        >
          <HarmonisationVariableDetails :variable="variable" />
        </ContentBlock>

        <ContentBlock
          v-if="variable.mappings?.length === 0"
          id="harmonisation-details-no-mapping"
          title="Harmonisation"
          description="No mapping found for this variable"
        >
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
