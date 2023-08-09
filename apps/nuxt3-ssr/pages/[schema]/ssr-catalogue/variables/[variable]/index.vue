<script setup lang="ts">
import { gql } from "graphql-request";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Variables($name: String) {
    Variables(filter: { name: { equals: [$name] } }) {
      name
      label
      description
      unit {
        name
      }
      format {
        name
      }
      mappings {
        source {
          id
          name
        }
        match {
          name
        }
        sourceDataset {
          name
        }
      }
      repeats {
        name
        mappings {
          source {
            id
            name
          }
          match {
            name
          }
          sourceDataset {
            name
          }
        }
      }
    }
    RepeatedVariables_agg(
      filter: { isRepeatOf: { name: { equals: [$name] } } }
    ) {
      count
    }
  }
`;

const variables = { name: route.params.variable };
let variable: IVariable;

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables },
  }
);

watch(data, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  variable = data?.data?.Variables[0];
  variable.nRepeats = data?.data?.RepeatedVariables_agg.count;
}

let tocItems = reactive([
  { label: "Description", id: "description" },
  { label: "Harmonization status", id: "harmonization-per-cohort" },
]);
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader :title="variable?.name" :description="variable?.label">
        <template #prefix>
          <BreadCrumbs
            :crumbs="{
              Home: `/${route.params.schema}/ssr-catalogue`,
              Variables: `/${route.params.schema}/ssr-catalogue/variables`,
            }"
          />
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
          <DefinitionList
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
                content: variable?.nRepeats > 0 ? variable?.nRepeats : 'None',
              },
            ]"
          >
          </DefinitionList>
        </ContentBlock>
        <ContentBlock
          id="harmonization-per-cohort"
          title="Harmonization status per Cohort"
          description="Overview of the harmonization status per Cohort"
        >
          <div class="grid grid-cols-3 gap-4">
            <div
              v-for="mapping in variable?.mappings"
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
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
