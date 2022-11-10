<script setup>
const query = `
  query Cohorts ($pid: String) {
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
    }
  }`;
const route = useRoute();
const variables = { pid: route.params.cohort };
const resp = await fetchGql(
  "catalogue/catalogue/graphql",
  query,
  variables
).catch((error) => console.log(error));
const cohort = resp?.data?.Cohorts[0];
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="cohort?.name"
        :description="cohort?.institution?.acronym"
      >
        <template #prefix>
          <BreadCrumbs />
        </template>
        <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template>
      </PageHeader>
    </template>
    <template #side> <SideNavigation /> </template>
    <template #main>
      <ContentBlocks>
        <ContentBlockIntro
          :image="cohort?.logo?.url"
          :link="cohort?.website"
          :contact="`mailto:${cohort?.contactEmail}`"
        />

        <ContentBlockDescription
          title="Description"
          :description="cohort?.description"
        />
        <ContentBlockGeneralDesign title="General Design" />
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

