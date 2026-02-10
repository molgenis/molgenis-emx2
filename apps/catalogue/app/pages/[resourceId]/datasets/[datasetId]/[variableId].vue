<script setup lang="ts">
import variableQuery from "../../../../gql/variable";
import type { IVariables } from "../../../../../interfaces/catalogue";
import { buildFilterFromKeysObject } from "../../../../../../metadata-utils/src";
import { useRoute } from "#app/composables/router";
import { moduleToString, useCatalogueContext } from "#imports";
import { useFetch, useHead, useRuntimeConfig } from "#app";
import HarmonisationListPerVariable from "../../../../components/harmonisation/HarmonisationListPerVariable.vue";
import HarmonisationGridPerVariable from "../../../../components/harmonisation/HarmonisationGridPerVariable.vue";
import HarmonisationVariableDetails from "../../../../components/harmonisation/VariableDetails.vue";
import ContentBlock from "../../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import ContentBlocks from "../../../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import LayoutsDetailPage from "../../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import SideNavigation from "../../../../components/SideNavigation.vue";
import CatalogueItemList from "../../../../components/CatalogueItemList.vue";

import {
  useQueryParams,
  calcIndividualVariableHarmonisationStatus,
} from "#imports";
import { computed, reactive } from "vue";
import type { Crumb } from "../../../../../../tailwind-components/types/types";

const route = useRoute();
const config = useRuntimeConfig();
const schema = config.public.schema as string;
const { buildBreadcrumbs, resourceUrl } = useCatalogueContext();

const query = moduleToString(variableQuery);
const resourceId = route.params.resourceId as string;
const { key } = useQueryParams();
const variableFilter = buildFilterFromKeysObject(key);
const resourceFilter = {
  _or: [
    { resources: { equals: [{ id: resourceId }] } },
    {
      resources: {
        partOfResources: { id: { equals: resourceId } },
      },
    },
  ],
};

const { data } = await useFetch(`/${schema}/graphql`, {
  method: "POST",
  body: { query, variables: { variableFilter, resourceFilter } },
});

const variable = computed(() => data.value.data.Variables[0]);
const resources = computed(() => data.value.data.Resources as { id: string }[]);
const isRepeating = computed(() => variable.value.repeatUnit?.name);

const crumbs: Crumb[] = buildBreadcrumbs([
  {
    label: resourceId,
    url: resourceUrl(resourceId),
  },
  {
    label: "Variables",
    url: "",
  },
  {
    label: variable.value?.name,
    url: "",
  },
]);

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

if (resourcesWithMapping.value.length === 0) {
  tocItems.push({
    label: "Harmonisation",
    id: "harmonisation-details-no-mapping",
  });
}

const titlePrefix = `${resourceId} `;
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

        <ContentBlock
          v-if="resourcesWithMapping.length === 0"
          id="harmonisation-details-no-mapping"
          title="Harmonisation"
          description="No mapping found for this variable"
        >
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
