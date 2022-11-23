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
        type{
          name
        }
        collectionType{
          name
        }
        populationAgeGroups{
          name
        }
        startYear
        endYear
        countries{
          name
        }
        numberOfParticipants
        designDescription
        design{
          definition
          name
        }
    }}`;
const variables = { pid: route.params.cohort };

let cohort: any = {};
const { data: cohortData, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables },
  }
);

watch(cohortData, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  cohort = data?.data?.Cohorts[0];
}
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="cohort?.name"
        :description="cohort?.institution?.acronym"
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
          :image="cohort?.logo?.url"
          :link="cohort?.website"
          :contact="`mailto:${cohort?.contactEmail}`"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="cohort?.description"
        />
        <ContentBlockGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :description="cohort?.designDescription"
          :cohort="cohort"
        />
        <ContentBlockAttachedFiles
          id="Files"
          title="Attached Files Generic Example"
        />
        <ContentBlockContact
          id="Contributers"
          title="Contact and Contributers"
        />
        <ContentBlockVariables
          id="Variables"
          title="Variables & Topics"
          description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockData
          id="AvailableData"
          title="Available Data & Samples"
          description="Explantation about variables and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockSubpopulations
          id="Subpopulations"
          title="Subpopulations"
          description="Explanation about subpopulations and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockCollectionEvents
          id="CollectionEvents"
          title="Collection Events"
          description="Explanation about collection events and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockNetwork
          id="Networks"
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
        <ContentBlockPartners
          id="Partners"
          title="Partners"
          description="Partners Explanation about networks from this cohort and the functionality seen here. similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
