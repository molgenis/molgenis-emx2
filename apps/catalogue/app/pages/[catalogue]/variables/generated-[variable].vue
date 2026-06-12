<script setup lang="ts">
import variableQuery from "../../../gql/variable";
import { buildFilterFromKeysObject } from "metadata-utils";
import { useRoute, useFetch, useHead, useRuntimeConfig } from "#app";
import { moduleToString } from "../../../../../tailwind-components/app/utils/moduleToString";
import { useQueryParams } from "../../../composables/useQueryParams";
import { calcIndividualVariableHarmonisationStatus } from "../../../utils/harmonisation";
import { computed, reactive } from "vue";
import LayoutsDetailPage from "../../../components/layouts/DetailPage.vue";
import PageHeader from "../../../../../tailwind-components/app/components/PageHeader.vue";
import SideNavigation from "../../../components/SideNavigation.vue";
import BreadCrumbs from "../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import ContentBlocks from "../../../../../tailwind-components/app/components/content/ContentBlocks.vue";
import ContentBlock from "../../../../../tailwind-components/app/components/content/ContentBlock.vue";
import CatalogueItemList from "../../../components/CatalogueItemList.vue";
import HarmonisationListPerVariable from "../../../components/harmonisation/HarmonisationListPerVariable.vue";
import HarmonisationGridPerVariable from "../../../components/harmonisation/HarmonisationGridPerVariable.vue";
import HarmonisationVariableDetails from "../../../components/harmonisation/VariableDetails.vue";
import fetchTableData from "../../../../../tailwind-components/app/composables/fetchTableData";
import { toSections } from "../../../utils/sectionsUtils";
import fetchMetadata from "../../../../../tailwind-components/app/composables/fetchMetadata";
import { isEmpty } from "../../../utils/fieldUtils";

const route = useRoute();
const config = useRuntimeConfig();
const schema = config.public.schema as string;
const { key } = useQueryParams();
const scoped = route.params.catalogue !== "all";
const catalogueRouteParam = route.params.catalogue as string;

const crumbs = [
  { label: `${route.params.catalogue}`, url: `/${route.params.catalogue}` },
  { label: "variables", url: `/${route.params.catalogue}/variables` },
  { label: route.params.variable as string, url: "" },
];

const TABLE_NAME = "Variables";

// fetch the resource data
const variableFilter = buildFilterFromKeysObject(key);
const data = await fetchTableData(schema, TABLE_NAME, {
  offset: 0,
  limit: 1,
  filter: variableFilter,
});
const variable = data.rows[0] || {};

const schemaMetaData = await fetchMetadata(schema);
const metadata = schemaMetaData.tables.find((table) => table.id === TABLE_NAME);
const sections = toSections(variable, metadata);

const resourcesWithMapping = computed(() => variable?.mappings || []);
const hasMappings = computed(
  () =>
    Array.isArray(resourcesWithMapping.value) &&
    resourcesWithMapping.value.length > 0
);
const isRepeating = computed(() => !!variable?.repeatUnit);

let tocItems = reactive([{ label: "General Information", id: "default" }]);

if (hasMappings.value) {
  tocItems.push({
    label: "Harmonisation status per source",
    id: "harmonisation-per-source",
  });
  tocItems.push({
    label: "Harmonisation details per source",
    id: "harmonisation-details-per-source",
  });
} else {
  tocItems.push({
    label: "Harmonisation",
    id: "harmonisation-details-no-mapping",
  });
}

const titlePrefix =
  route.params.catalogue === "all" ? "" : route.params.catalogue + " ";
const variableName = computed(() => String(variable?.name || ""));
useHead({
  title: titlePrefix + variableName.value,
  meta: [{ name: "description", content: String(variable?.description || "") }],
});
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="page-header"
        :title="variableName"
        :description="String(variable?.label ?? '')"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="variableName"
        :items="tocItems"
        header-target="#page-header"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="variable">
        <ContentBlock
          v-for="section in sections"
          :id="section.id"
          :title="section.label"
        >
          <ul v-if="section.fields.length > 0">
            <li
              v-for="field in section.fields.filter((f:any) => !isEmpty(f.value))"
              class="mb-2"
            >
              <strong>{{ field.label }}:</strong> {{ field.value }}
            </li>
          </ul>
        </ContentBlock>

        <!-- <ContentBlock
          v-if="hasMappings"
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
          v-if="hasMappings"
          id="harmonisation-details-per-cohort"
          title="Harmonisation details per source"
          description="Select a data source to see the details of the harmonisation"
        >
          <HarmonisationVariableDetails :variable="variable" />
        </ContentBlock>

        <ContentBlock
          v-if="!hasMappings"
          id="harmonisation-details-no-mapping"
          title="Harmonisation"
          description="No mapping found for this variable"
        >
        </ContentBlock> -->
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
