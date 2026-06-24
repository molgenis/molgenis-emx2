<script setup lang="ts">
import { useHead, useRuntimeConfig, navigateTo, useFetch } from "#app";
import { definePageMeta } from "#imports";
import { computed } from "vue";
import type {
  ICatalogues,
  ICatalogues_agg,
  INetworks_agg,
} from "../../interfaces/catalogue";
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

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const variablesFilter = {
  resource: {
    _or: [
      { mg_tableclass: { equals: `${schema}.Networks` } },
      { mg_tableclass: { equals: `${schema}.Catalogues` } },
    ],
  },
};

const query = computed(() => {
  return `
  query Catalogues($variablesFilter:VariablesFilter){
    Catalogues {
      id
      name
      acronym
      description
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
    Variables_agg(filter: $variablesFilter) {
      count
    }
    Collections_agg {
      count
    }
    Networks_agg {
      count
    }
  }
  `;
});

interface Resp<T, U> {
  data: {
    Catalogues: T[];
    Variables_agg: U;
    Collections_agg: INetworks_agg;
    Networks_agg: INetworks_agg;
  };
}

const graphqlURL = computed(
  () => `/${useRuntimeConfig().public.schema}/graphql`
);
const { data } = await useFetch<Resp<ICatalogues, ICatalogues_agg>>(
  graphqlURL.value,
  {
    method: "POST",
    body: {
      query,
      variables: {
        variablesFilter,
      },
    },
  }
);

const catalogues = data.value?.data?.Catalogues as ICatalogues[];
const groupedCatalogues = catalogues
  ? Object.groupBy(
      catalogues.filter((c) => !c.mainCatalogue),
      (c) => c.catalogueType?.name ?? "theme"
    )
  : { theme: [], project: [], organisation: [] };
Object.keys(groupedCatalogues).forEach((key) => {
  groupedCatalogues[key]?.sort((a, b) => a.id.localeCompare(b.id));
});

const projectAndOrganisationCatalogues = [
  ...(groupedCatalogues.project ?? []),
  ...(groupedCatalogues.organisation ?? []),
].sort((a, b) => (a.acronym || a.id).localeCompare(b.acronym || b.id));

const mainCatalogue = computed<ICatalogues | null>(() => {
  return catalogues?.find((catalogue) => catalogue.mainCatalogue) ?? null;
});

const pageDescription = computed(
  () =>
    mainCatalogue.value?.description ||
    `Welcome to the European Health Research Data and Samples Catalogue: a growing collaborative effort to integrate the catalogues of diverse EU research projects and networks to accelerate reuse and improve citizens' health. Click on the ‘Search All’ button to browse through all ${counts.value.collections} collections, ${counts.value.networks} networks and ${counts.value.variables} variables to find the data you are looking for, or select one of the individual catalogues below.`
);

const pageTitle = computed(
  () => mainCatalogue.value?.name || "Health Data and Samples Catalogue"
);

const counts = computed(() => {
  return {
    variables: data.value?.data.Variables_agg?.count ?? 0,
    collections: data.value?.data.Collections_agg?.count ?? 0,
    networks: data.value?.data.Networks_agg?.count ?? 0,
  };
});

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
              <Button label="Search all" size="large" />
            </NuxtLink>
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
      v-if="projectAndOrganisationCatalogues?.length"
      title="Project and organisation catalogues"
      description="Catalogues maintained by organisations and individual research projects or consortia:"
      :catalogues="projectAndOrganisationCatalogues"
    />
    <ContentBlock
      v-if="!catalogues?.length"
      title="No Catalogues found"
      description="Please add catalogues via admin user interface"
    />
  </LayoutsLandingPage>
</template>
