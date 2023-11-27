<script setup lang="ts">
import datasourceGql from "~~/gql/datasourceDetails";
const query = moduleToString(datasourceGql);
const route = useRoute();
const config = useRuntimeConfig();

const { data, error } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables: { id: route.params.datasource as string } },
  }
);

const dataSource = computed(() => {
  return data.value.data.DataSources[0];
});

let tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "description" },
    { label: "Overview", id: "overview" },
    { label: "Population", id: "population" },
    { label: "Contents", id: "contents" },
    { label: "Linkage", id: "linkage" },
  ];
  return tableOffContents;
});

useHead({ title: dataSource.value?.acronym || dataSource.value?.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.cohort}"}}}`;

const crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    `${route.params.catalogue}`
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
  crumbs[
    "Data sources"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/datasources`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue`;
  crumbs["Data sources"] = `/${route.params.schema}/ssr-catalogue/datasources`;
}
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="dataSource?.acronym || dataSource?.name"
        :description="dataSource?.acronym ? dataSource?.name : ''"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" :current="dataSource?.id" />
        </template>
        <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template>
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="dataSource?.acronym || dataSource?.name"
        :image="dataSource?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <div v-if="error">
        {{ error }}
      </div>
      <ContentBlocks v-if="dataSource">
        <ContentBlockIntro
          :image="dataSource?.logo?.url"
          :link="dataSource?.website"
          :contact="dataSource?.contactEmail"
          :contact-name="dataSource?.name"
          :contact-message-filter="messageFilter"
        />
        <ContentBlockDescription
          id="description"
          title="Description"
          :description="dataSource?.description"
        />

        <ContentBlock title="Overview" id="overview">
          <CatalogueItemList
            :items="[
              { label: 'Id', content: dataSource.name },
              { label: 'Acronym', content: dataSource.acronym },
              { label: 'Name', content: dataSource.name },
              { label: 'Type', content: dataSource.type, type: 'ONTOLOGY' },
              { label: 'Keywords', content: dataSource.keywords },
              { label: 'Website', content: dataSource.website },
              {
                label: 'Lead organisation',
                content: dataSource.leadOrganisation[0].name,
              },
              { label: 'Description', content: dataSource.description },
              {
                label: 'Date established',
                content: dataSource.dateEstablished,
              },
              {
                label: 'Start data collection',
                content: dataSource.startDataCollection,
              },
              { label: 'Logo', content: dataSource.logo },
            ]"
          />
        </ContentBlock>

        <ContentBlock title="Population" id="population">
          <CatalogueItemList
            :items="[
              {
                label: 'Number of participants',
                content: dataSource.numberOfParticipants,
              },
              {
                label: 'Countries',
                content: dataSource.countries,
                type: 'ONTOLOGY',
              },
              {
                label: 'Population age groups',
                content: dataSource.populationAgeGroups,
                type: 'ONTOLOGY',
              },
              {
                label: 'Population entry',
                content: dataSource.populationEntry,
                type: 'ONTOLOGY',
              },
              {
                label: 'Population exit other',
                content: dataSource.populationExitOther,
              },
              {
                label: 'Population disease',
                content: dataSource.populationAgeGroups,
                type: 'ONTOLOGY',
              },
            ]"
          />
        </ContentBlock>

        <ContentBlock title="Contents" id="contents">
          <CatalogueItemList
            :items="[
              {
                label: 'Datasets',
                content: dataSource.datasets,
              },
              {
                label: 'Areas of information',
                content: dataSource.areasOfInformation,
                type: 'ONTOLOGY',
              },
              {
                label: 'Quality of life other',
                content: dataSource.qualityOfLifeOther,
              },
              {
                label: 'Languages',
                content: dataSource.languages,
                type: 'ONTOLOGY',
              },
              {
                label: 'Record trigger',
                content: dataSource.recordTrigger,
              },
            ]"
          />
        </ContentBlock>

        <ContentBlock title="Linkage" id="linkage">
          <CatalogueItemList
            :items="[
              {
                label: 'Linkage possibility',
                content: dataSource.linkagePossibility,
              },
              {
                label: 'Linkage description',
                content: dataSource.linkageDescription,
              },
              {
                label: 'Linked resources',
                content: dataSource.linkedResources,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="debug" v-if="(route.query.debug as string)">
          <pre>{{ dataSource }}</pre>
        </ContentBlock>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
