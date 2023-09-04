<script setup lang="ts">
import { gql } from "graphql-request";
import { IResource } from "interfaces/types";
const config = useRuntimeConfig();
const route = useRoute();

const resourceName: string = route.params.resourceType as string;
const resourceType: string =
  resourceName.charAt(0).toUpperCase() + resourceName.slice(1);

const query = gql`
  query ${resourceType}($id: String) {
    ${resourceType}( filter: { id: { equals: [$id] } } ) {
      id
      name
      acronym
      description
    }
  }`;

let resource: IResource;

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables: { id: route.params.resource } },
  }
);

watch(data, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  resource = data?.data?.[resourceType][0];
}

let tocItems = reactive([
  { label: "Description", id: "description" },
  { label: "Harmonization status", id: "harmonization-per-cohort" },
]);

let crumbs: Record<string, string> = {
  Home: `/${route.params.schema}/ssr-catalogue`,
};
crumbs[resourceType] = `/${route.params.schema}/ssr-catalogue/${resourceName}`;
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="resource?.name" :description="resource?.label">
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation :title="resource?.name" :items="tocItems" />
    </template>
    <template #main>
      <ContentBlocks v-if="resource">
        <ContentBlock
          id="description"
          title="Description"
          :description="resource?.description"
        >
          <!-- <DefinitionList
            :items="[
              {
                label: 'Unit',
                content: resource?.unit?.name,
              },
              {
                label: 'Format',
                content: resource?.format?.name,
              },
              {
                label: 'N repeats',
                content: resource?.nRepeats > 0 ? resource?.nRepeats : 'None',
              },
            ]"
          > 
          </DefinitionList> -->
        </ContentBlock>
        <!-- <ContentBlock
          id="harmonization-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <div class="grid grid-cols-3 gap-4">
            <div
              v-for="mapping in resource?.mappings"
              class="inline-flex gap-1 group text-icon text-breadcrumb-arrow"
            >
              <BaseIcon name="completed" :width="24" class="text-green-500" />
              <NuxtLink
                :to="`/${route.params.schema}/ssr-catalogue/cohorts/${mapping.source.id}`"
              >
                <span class="text-body-base text-blue-500 hover:underline">{{
                  mapping.source.id
                }}</span>
              </NuxtLink>
            </div>
          </div>
        </ContentBlock> -->
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
