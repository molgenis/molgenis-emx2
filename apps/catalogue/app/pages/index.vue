<script setup lang="ts">
import { useHead, useRuntimeConfig, navigateTo, useFetch } from "#app";
import { definePageMeta } from "#imports";
import { computed } from "vue";
import type { IResources, IResources_agg } from "../../interfaces/catalogue";
import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import ContentBlockCatalogues from "../components/content/ContentBlockCatalogues.vue";
import ContentBlock from "../../../tailwind-components/app/components/content/ContentBlock.vue";

//add redirect middleware for cohortOnly to skip this page
definePageMeta({
  middleware: [
    function (to) {
      const cohortOnly =
        to.query["cohort-only"] === "true" ||
        useRuntimeConfig().public.cohortOnly;
      if (cohortOnly) {
        return navigateTo(`/all`, {
          replace: true,
        });
      }
    },
  ],
});

const query = computed(() => {
  return `
  query Resources{
    Resources(filter:{type:{name:{equals:"Catalogue"}}}) {
      id
      name
      acronym
      description
      type {
        name
      }
      catalogueType {
        name
      }
      mainCatalogue
      logo {
        id
        size
        extension
        url
      }
    }
  }
  `;
});

interface Resp<T, U> {
  data: Record<string, T[]>;
  data_agg: Record<string, U>;
}

const graphqlURL = computed(
  () => `/${useRuntimeConfig().public.schema}/graphql`
);
const { data } = await useFetch<Resp<IResources, IResources_agg>>(
  graphqlURL.value,
  {
    method: "POST",
    body: { query },
  }
);

const catalogues = (data.value?.data?.Resources as IResources[])?.filter(c => !c.mainCatalogue);
const groupedCatalogues = catalogues
  ? Object.groupBy(catalogues, (c) => c.catalogueType?.name ?? "theme")
  : { theme: [], project: [], organisation: [] };
Object.keys(groupedCatalogues).forEach((key) => {
  groupedCatalogues[key]?.sort((a, b) => a.id.localeCompare(b.id));
});

const mainCatalogue = computed<IResources | null>(() => {
  return catalogues?.find((catalogue) => catalogue.mainCatalogue) ?? null;
});

const pageDescription = computed(
  () =>
    mainCatalogue.value?.description ||
    "A collaborative effort to integrate the catalogues of diverse EU research projects and networks to accelerate reuse and improve citizens health."
);

const pageTitle = computed(
  () => mainCatalogue.value?.name || "Health Data and Samples Catalogue"
);

useHead(() => ({
  title: pageTitle.value,
  meta: [
    {
      name: "description",
      content: pageDescription.value,
    },
  ],
}));
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      :title="pageTitle"
      :description="pageDescription"
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        >
          <div class="flex flex-col items-center max-w-sm lg:mt-5">
            <NuxtLink :to="`/all`">
              <Button label="Search all" />
            </NuxtLink>
            <p
              class="mt-1 mb-0 text-center lg:mt-10 text-body-lg"
              v-if="catalogues?.length"
            >
              or select a specific catalogue below:
            </p>
          </div>
        </div>
      </template>
    </PageHeader>
    <ContentBlockCatalogues
      v-if="groupedCatalogues?.theme?.length"
      title="Thematic catalogues"
      description="Catalogues focused on a particular theme, developed by a collaboration of projects, networks and/or organisations:"
      :catalogues="groupedCatalogues?.theme ?? []"
    />
    <ContentBlockCatalogues
      v-if="groupedCatalogues?.project?.length"
      title="Project catalogues"
      description="Catalogues maintained by individual research projects or consortia:"
      :catalogues="groupedCatalogues?.project"
    />
    <ContentBlockCatalogues
      v-if="groupedCatalogues?.organisation?.length"
      title="Organisation catalogues"
      description="Catalogues maintained by organisations:"
      :catalogues="groupedCatalogues?.organisation"
    />
    <ContentBlock
      v-if="!catalogues?.length"
      title="No Catalogues found"
      description="Please add catalogues via admin user interface"
    />
  </LayoutsLandingPage>
</template>
