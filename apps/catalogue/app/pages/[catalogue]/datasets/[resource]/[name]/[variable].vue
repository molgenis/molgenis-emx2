<script setup lang="ts">
import { useRoute, useRuntimeConfig, useHead } from "#app";
import { computed } from "vue";
import type { Crumb } from "../../../../../../../tailwind-components/types/types";
import LayoutsLandingPage from "../../../../../components/layouts/LandingPage.vue";
import PageHeader from "../../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Emx2RecordView from "../../../../../../../tailwind-components/app/components/display/Emx2RecordView.vue";

const config = useRuntimeConfig();
const route = useRoute();

const schema = config.public.schema as string;
const catalogue = route.params.catalogue as string;
const resourceId = route.params.resource as string;
const datasetName = route.params.name as string;
const variableName = route.params.variable as string;

useHead({
  title: `${variableName} - Variable`,
  meta: [
    {
      name: "description",
      content: `Variable ${variableName} from dataset ${datasetName}`,
    },
  ],
});

const rowId = computed(() => ({
  dataset: {
    resource: { id: resourceId },
    name: datasetName,
  },
  name: variableName,
}));

const crumbs: Crumb[] = [
  { label: catalogue, url: `/${catalogue}` },
  { label: "datasets", url: `/${catalogue}/datasets` },
  { label: resourceId, url: `/${catalogue}/datasets/${resourceId}` },
  { label: datasetName, url: `/${catalogue}/datasets/${resourceId}/${datasetName}` },
  { label: variableName, url: "" },
];
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader :title="variableName">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" />
      </template>
    </PageHeader>

    <Emx2RecordView
      :schema-id="schema"
      table-id="Variables"
      :row-id="rowId"
    />
  </LayoutsLandingPage>
</template>
