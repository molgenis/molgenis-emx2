<script setup lang="ts">
import variableQuery from "../../../../../../../gql/variable";
import type { IVariable, IVariableMappings } from "~/interfaces/types";
import { buildFilterFromKeysObject } from "metadata-utils";
import { useRoute } from "#app/composables/router";
import { moduleToString } from "../../../../../../../../tailwind-components/utils/moduleToString";
import { useFetch, useHead } from "#app";
import {
  useQueryParams,
  calcIndividualVariableHarmonisationStatus,
} from "#imports";
import { computed, reactive } from "vue";
const route = useRoute();

const query = moduleToString(variableQuery);
const scoped = route.params.catalogue !== "all";
const catalogueRouteParam = route.params.catalogue as string;
const { key } = useQueryParams();
const variableFilter = buildFilterFromKeysObject(key);
const resourceFilter = scoped
  ? {
      _or: [
        { resources: { equals: [{ id: catalogueRouteParam }] } },
        {
          resources: {
            partOfResources: { id: { equals: catalogueRouteParam } },
          },
        },
      ],
    }
  : {};

type VariableDetailsWithMapping = IVariable &
  IVariableMappings & { nRepeats: number };

const { data } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  body: { query, variables: { variableFilter, resourceFilter } },
});

const variable = computed(
  () => data.value.data.Variables[0] as VariableDetailsWithMapping
);
const resources = computed(() => data.value.data.Resources as { id: string }[]);
const isRepeating = computed(() => variable.value.repeatUnit?.name);

let crumbs: any = {};
crumbs[
  `${route.params.catalogue}`
] = `/${route.params.schema}/catalogue/${route.params.catalogue}`;
crumbs[
  route.params.resourceType as string
] = `/${route.params.schema}/catalogue/${route.params.catalogue}/${route.params.resourceType}`;
crumbs[
  route.params.resource as string
] = `/${route.params.schema}/catalogue/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}#Variables`;
crumbs["variables"] = "";
crumbs[variable.value?.name] = "";

const resourcesWithMapping = computed(() => {
  if (!resources.value) return [];
  return resources.value
    .map((resource) => {
      const status = calcIndividualVariableHarmonisationStatus(variable.value, [
        resource,
      ])[0];
      return {
        resource,
        status,
      };
    })
    .filter(({ status }) =>
      Array.isArray(status)
        ? status.filter((s) => s !== "unmapped").length
        : status !== "unmapped"
    );
});

let tocItems = reactive([{ label: "Definition", id: "definition" }]);

if (resourcesWithMapping.value.length > 0) {
  tocItems.push({
    label: "Harmonisation status per source",
    id: "harmonisation-per-source",
  });
  tocItems.push({
    label: "Harmonisation details per source",
    id: "harmonisation-details-per-source",
  });
}

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
useHead({
  title: titlePrefix + variable.value.name,
  meta: [
    {
      name: "description",
      content: variable.value.description,
    },
  ],
});
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="page-header"
        :title="variable?.name"
        :description="variable?.label"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template v-if="tocItems.length > 1" #side>
      <SideNavigation
        :title="variable?.name"
        :items="tocItems"
        header-target="#page-header"
      />
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
              {
                label: 'Dataset',
                content: variable?.dataset?.name,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock
          v-if="resourcesWithMapping.length > 0"
          id="harmonisation-per-source"
          title="Harmonisation status per source"
        >
          <HarmonisationGridPerVariable
            v-if="isRepeating"
            :variable="variable"
          />
          <HarmonisationListPerVariable v-else :mappings="variable.mappings" />
        </ContentBlock>

        <ContentBlock
          v-if="resourcesWithMapping.length > 0"
          id="harmonisation-details-per-cohort"
          title="Harmonisation details per source"
          description="Select a data source to see the details of the harmonisation"
        >
          <HarmonisationVariableDetails :variable="variable" />
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
