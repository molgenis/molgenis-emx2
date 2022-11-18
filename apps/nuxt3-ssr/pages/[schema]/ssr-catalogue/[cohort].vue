<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();

const query = `query Cohorts ($pid: String){
    Cohorts(filter: { pid: { equals: [$pid] } }){
        name
        description
        website
        logo{
          url
        }
        contactEmail
        institution{
          acronym
        }
    }}`;
const variables = { pid: route.params.cohort };

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables },
  }
);
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="data?.data?.Cohorts[0]?.name"
        :description="data?.data?.Cohorts[0]?.institution?.acronym"
      >
        <template #prefix>
          <BreadCrumbs
            :crumbs="{
              Home: `/${route.params.schema}/ssr-catalogue`,
              Cohorts: `/${route.params.schema}/ssr-catalogue`,
            }"
          />
        </template>
        <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation />
    </template>
    <template #main>
      <ContentBlocks>
        <ContentBlockIntro
          :image="data?.data?.Cohorts[0]?.logo?.url"
          :link="data?.data?.Cohorts[0]?.website"
          :contact="`mailto:${data?.data?.Cohorts[0]?.contactEmail}`"
        />
        <ContentBlockDescription
          title="Description"
          :description="data?.data?.Cohorts[0]?.description"
        />
        <ContentBlockGeneralDesign title="General Design" description="" />
        <ContentBlockAttachedFiles title="Attached Files Generic Example" />
        <ContentBlockContact title="Contact and Contributers" />
        <ContentBlockVariables
          title="Variables & Topics"
          description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockData
          title="Available Data & Samples"
          description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockSubpopulations
          title="Subpopulations"
          description="Explanation about subpopulations and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockCollectionEvents
          title="Collection Events"
          description="Explanation about collection events and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockNetwork
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockPartners
          title="Partners"
          description="Partners Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>

