<script setup lang="ts">
import datasourceGql from "~~/gql/datasourceDetails";
import datasetQuery from "~~/gql/datasets";
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
    { label: "Datasets", id: "datasets" },
    { label: "Contents", id: "contents" },
    { label: "Linkage", id: "linkage" },
    { label: "Access", id: "access" },
    { label: "Updates", id: "updates" },
    { label: "Quality", id: "quality" },
    { label: "Standards", id: "standards" },
    { label: "Information", id: "information" },
    { label: "Collaborations", id: "collaborations" },
  ];
  return tableOffContents;
});

useHead({ title: dataSource.value?.acronym || dataSource.value?.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.datasource}"}}}`;

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

function datasetMapper(item: { name: string; description: string }) {
  return {
    id: item.name,
    name: item.name,
    description: item.description,
    _path: `/${route.params.schema}/ssr-catalogue/all/datasets`,
  };
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
              { label: 'Acronym', content: dataSource.acronym },
              { label: 'Name', content: dataSource.name },
              { label: 'Type', content: dataSource.type, type: 'ONTOLOGY' },
              { label: 'Keywords', content: dataSource.keywords },
              { label: 'Website', content: dataSource.website },
              {
                label: 'Lead organisation',
                content: dataSource.leadOrganisation[0].name,
              },
              {
                label: 'Date established',
                content: dataSource.dateEstablished,
              },
              {
                label: 'Start data collection',
                content: dataSource.startDataCollection,
              },
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
                content: dataSource.populationDisease,
                type: 'ONTOLOGY',
              },
            ]"
          />
        </ContentBlock>

        <TableContent
          id="datasets"
          title="Datasets"
          description="List of datasets for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description' },
          ]"
          type="Datasets"
          :query="datasetQuery"
          :filter="{ id: route.params.datasource }"
          :rowMapper="datasetMapper"
          v-slot="slotProps"
        >
          <DatasetDisplay :id="slotProps.id" />
        </TableContent>

        <ContentBlock title="Contents" id="contents">
          <CatalogueItemList
            :items="[
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

        <ContentBlock title="Access" id="access">
          <CatalogueItemList
            :items="[
              {
                label: 'Data holder',
                content: dataSource.dataHolder?.name,
              },
              {
                label: 'DAPs',
                content: dataSource.dAPs,
              },
              {
                label: 'Informed consent',
                content: dataSource.informedConsent,
                type: 'ONTOLOGY',
              },
              {
                label: 'Access identifiable data',
                content: dataSource.accessIdentifiableData,
              },
              {
                label: 'Access identifiable data route',
                content: dataSource.accessIdentifiableDataRoute,
              },
              {
                label: 'Access subject details',
                content: dataSource.accessSubjectDetails,
              },
              {
                label: 'Audit possible',
                content: dataSource.auditPossible,
              },
              {
                label: 'Standard operating procedures',
                content: dataSource.standardOperatingProcedures,
              },
              {
                label: 'Biospecimen access',
                content: dataSource.biospecimenAccess,
              },
              {
                label: 'Biospecimen access conditions',
                content: dataSource.biospecimenAccessConditions,
              },
              {
                label: 'Governance details',
                content: dataSource.governanceDetails,
              },
              {
                label: 'Approval for publication',
                content: dataSource.approvalForPublication,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="Updates" id="updates">
          <CatalogueItemList
            :items="[
              {
                label: 'Preservation',
                content: dataSource.preservation,
              },
              {
                label: 'Preservation duration',
                content: dataSource.preservationDuration,
              },
              {
                label: 'Refresh period',
                content: dataSource.refreshPeriod,
                type: 'ONTOLOGY',
              },
              {
                label: 'DateLast refresh',
                content: dataSource.dateLastRefresh,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="Quality" id="quality">
          <CatalogueItemList
            :items="[
              {
                label: 'Qualification',
                content: dataSource.qualification,
              },
              {
                label: 'Qualifications description',
                content: dataSource.qualificationsDescription,
              },
              {
                label: 'Access for validation',
                content: dataSource.accessForValidation,
              },
              {
                label: 'Quality validation frequency',
                content: dataSource.qualityValidationFrequency,
              },
              {
                label: 'Quality validation methods',
                content: dataSource.qualityValidationMethods,
              },
              {
                label: 'Correction methods',
                content: dataSource.correctionMethods,
              },
              {
                label: 'Quality validation results',
                content: dataSource.qualityValidationResults,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="standards" id="standards">
          <CatalogueItemList
            :items="[
              {
                label: 'Cdms',
                content: dataSource.cdms,
              },
              {
                label: 'Cdms other',
                content: dataSource.cdmsOther,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="information" id="information">
          <CatalogueItemList
            :items="[
              {
                label: 'Design paper',
                content: dataSource.designPaper,
              },
              {
                label: 'Publications',
                content: dataSource.publications,
              },
              {
                label: 'DesignPaper',
                content: dataSource.designPaper,
              },
              {
                label: 'Informed consent type',
                content: dataSource.informedConsentType?.name,
              },
              {
                label: 'Funding sources',
                content: dataSource.fundingSources?.map((fundingSource: any) => {
                  return fundingSource.name;
                }),
              },
              {
                label: 'Funding statement',
                content: dataSource.fundingStatement,
              },
              {
                label: 'Acknowledgements',
                content: dataSource.acknowledgements,
              },
              {
                label: 'Documentation',
                content: dataSource.documentation,
              },
              {
                label: 'Supplementary Information',
                content: dataSource.supplementaryInformation,
              },
            ]"
          >
          </CatalogueItemList>
        </ContentBlock>

        <ContentBlock title="collaborations" id="collaborations">
          <CatalogueItemList
            :items="[
              {
                label: 'Networks',
                content: dataSource.networks?.map((network: any) => {
                  return `${network.name} (${network.id})`;
                })
              },
              {
                label: 'Studies',
                content: dataSource.studies?.map((study: any) => {
                  return `${study.name} (${study.id})`;
                }),
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
