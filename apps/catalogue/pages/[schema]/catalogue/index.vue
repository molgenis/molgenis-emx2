<script setup lang="ts">
import {
  useRoute,
  useHead,
  useRuntimeConfig,
  navigateTo,
  useFetch,
} from "#app";
import { definePageMeta } from "#imports";
import { computed } from "vue";
import type { IResources, IResources_agg } from "~/interfaces/catalogue";

const pageDescription =
  "A collaborative effort to integrate the catalogues of diverse EU research projects and networks to accelerate reuse and improve citizens health.";

const route = useRoute();
useHead({
  title: "Health Data and Samples Catalogue",
  meta: [
    {
      name: "description",
      content: pageDescription,
    },
  ],
});

//add redirect middleware for cohortOnly to skip this page
definePageMeta({
  middleware: [
    function (to) {
      const cohortOnly =
        to.query["cohort-only"] === "true" ||
        useRuntimeConfig().public.cohortOnly;
      if (cohortOnly) {
        return navigateTo(`/${to.params.schema}/catalogue/all`, {
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

let graphqlURL = computed(() => `/${route.params.schema}/graphql`);
const { data } = await useFetch<Resp<IResources, IResources_agg>>(
  graphqlURL.value,
  {
    method: "POST",
    body: { query },
  }
);

const thematicCatalogues = computed(() => {
  let result = data?.value?.data?.Resources
    ? data.value?.data?.Resources?.filter(
        (c) => c.catalogueType?.name !== "project"
      )
    : [];
  result.sort((a, b) => a.id.localeCompare(b.id));
  return result;
});
const projectCatalogues = computed(() => {
  let result = data?.value?.data?.Resources
    ? data.value?.data?.Resources?.filter(
        (c) => c.catalogueType?.name === "project"
      )
    : [];
  result.sort((a, b) => a.id.localeCompare(b.id));
  return result;
});
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      title="European Health Research Data and Sample Catalogue"
      :description="pageDescription"
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        >
          <div class="flex flex-col items-center max-w-sm lg:mt-5">
            <NuxtLink :to="`/${route.params.schema}/catalogue/all`">
              <Button label="Search all" />
            </NuxtLink>
            <p
              class="mt-1 mb-0 text-center lg:mt-10 text-body-lg"
              v-if="
                thematicCatalogues.length > 0 || projectCatalogues.length > 0
              "
            >
              or select a specific catalogue below:
            </p>
          </div>
        </div>
      </template>
    </PageHeader>
    <ContentBlockCatalogues
      v-if="thematicCatalogues?.length > 0"
      title="Thematic catalogues"
      description="Catalogues focused on a particular theme, developed by a collaboration of projects, networks and/or organisations:"
      :catalogues="thematicCatalogues"
    />
    <ContentBlockCatalogues
      v-if="projectCatalogues?.length > 0"
      title="Project catalogues"
      description="Catalogues maintained by individual research projects or consortia:"
      :catalogues="projectCatalogues"
    />
    <ContentBlock
      v-if="projectCatalogues.length === 0 && thematicCatalogues.length === 0"
      title="No Catalogues found"
      description="Please add catalogues via admin user interface"
    />
  </LayoutsLandingPage>
</template>
