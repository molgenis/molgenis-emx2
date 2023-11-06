<script setup lang="ts">
import { gql } from "graphql-request";
import cohortsQuery from "~~/gql/cohorts";
import datasourcesQuery from "~~/gql/datasources";
import variablesQuery from "~~/gql/variables";
import fileFragment from "~~/gql/fragments/file";
const config = useRuntimeConfig();
const route = useRoute();

const networkVariablesCount = ref(0);
const networkVariablesFilter = ref({});
const query = gql`
  query Networks($id: String) {
    Networks(filter: { id: { equals: [$id] } }) {
      id
      name
      description
      website
      logo ${moduleToString(fileFragment)}
      acronym
      contacts {
        roleDescription
        firstName
        lastName
        prefix
        initials
        email
        title {
          name
        }
        organisation {
          name
        }
      }
      leadOrganisation {
         name
      }
      startYear
      endYear
      fundingStatement
      acknowledgements
      additionalOrganisations {
        name
      }
      models {
        id
      }
      cohorts_agg{count}
      cohorts {
        id, name, type {name}, description
      }
      dataSources_agg{count}
      dataSources {
        id, name, type {name}, description
      }
      databanks {
        id, name, type {name}  description
      }
    }
  }
`;
const variables = route.params.network
  ? { id: route.params.network }
  : { id: route.params.catalogue };

let network: INetwork;

const {
  data: networkData,
  pending,
  error,
  refresh,
} = await useFetch(`/${route.params.schema}/catalogue/graphql`, {
  baseURL: config.public.apiBase,
  method: "POST",
  body: { query, variables },
});

watch(networkData, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  network = data?.data?.Networks[0];
  if (network?.models?.length > 0) {
    fetchVariableCount(network.models);
  }
}

async function fetchVariableCount(models: { id: string }[]) {
  const modelFilters = models.map((model) => ({
    dataset: { resource: { id: { equals: model.id } } },
  }));
  networkVariablesFilter.value = { filter: { _and: { _or: modelFilters } } };
  const query = gql`
    query Variables_agg($filter: VariablesFilter) {
      Variables_agg(filter: $filter) {
        count
      }
    }
  `;
  const { data } = await useFetch(`/${route.params.schema}/catalogue/graphql`, {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query, variables: networkVariablesFilter.value },
  });

  networkVariablesCount.value = data.value.data.Variables_agg.count;
}

let tocItems = computed(() => {
  let tableOffContents = [{ label: "Description", id: "Description" }];
  if (network?.documentation) {
    tableOffContents.push({ label: "Attached files", id: "Files" });
  }
  if (network?.contacts) {
    tableOffContents.push({
      label: "Contact & contributors",
      id: "Contributors",
    });
  }
  if (network?.collectionEvents) {
    tableOffContents.push({
      label: "Available data & samples",
      id: "AvailableData",
    });
  }
  // { label: 'Variables & topics', id: 'Variables' },
  if (network?.additionalOrganisations) {
    tableOffContents.push({ label: "Partners", id: "Partners" });
  }

  if (
    network?.dataAccessConditions?.length ||
    network?.dataAccessConditionsDescription ||
    network?.releaseDescription
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (network?.fundingStatement || network?.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Citation requirements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (network?.cohorts_agg.count > 0) {
    tableOffContents.push({ label: "Cohorts", id: "cohorts" });
  }
  if (network?.dataSources_agg.count > 0) {
    tableOffContents.push({ label: "Data sources", id: "datasources" });
  }

  if (networkVariablesCount.value > 0) {
    tableOffContents.push({ label: "Variables", id: "variables" });
  }

  return tableOffContents;
});

let accessConditionsItems = computed(() => {
  let items = [];
  if (network?.dataAccessConditions?.length) {
    items.push({
      label: "Conditions",
      content: cohort.dataAccessConditions.map((c) => c.name),
    });
  }
  if (network?.releaseDescription) {
    items.push({
      label: "Release",
      content: network.releaseDescription,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (network?.fundingStatement) {
    items.push({
      label: "Funding",
      content: network.fundingStatement,
    });
  }
  if (network?.acknowledgements) {
    items.push({
      label: "Citation requirements ",
      content: network.acknowledgements,
    });
  }

  return items;
});

function cohortMapper(cohort: {
  id: string;
  acronym: string;
  name: string;
  design: { name: string };
  numberOfParticipants: number;
}) {
  return {
    id: cohort.id,
    name: cohort.name,
    design: cohort.design?.name,
    numberOfParticipants: cohort.numberOfParticipants,
    _renderComponent: "CohortDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/cohorts/${cohort.id}`,
  };
}

function dataSourcesMapper(dataSource: {
  id: string;
  acronym: string;
  name: string;
  numberOfParticipants: string;
  type: { name: string };
}) {
  return {
    id: dataSource.id,
    name: dataSource.name,
    type: dataSource.type?.name,
    numberOfParticipants: dataSource.numberOfParticipants,
    _renderComponent: "DataSourceDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/datasources/${dataSource.id}`,
  };
}

function variableMapper(variable: {
  name: string;
  label: string;
  resource: {
    id: string;
  };
}) {
  return {
    id: variable.name,
    name: variable.name,
    label: variable.label,
    model: variable.resource.id,
    _renderComponent: "VariableDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/variables/${variable.resource.id}`,
  };
}

useHead({ title: network?.acronym || network?.name });

const crumbs = {};
if (route.params.catalogue) {
  crumbs[
    `${route.params.catalogue}`
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
  crumbs[
    "about"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/about`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue`;
  crumbs["Networks"] = `/${route.params.schema}/ssr-catalogue/networks`;
}
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="network?.acronym || network?.name"
        :description="network?.acronym ? network?.name : ''"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="network.acronym"
        :image="network?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="network">
        <ContentBlockIntro
          :image="network?.logo?.url"
          :link="network?.website"
          :contact="network?.contactEmail"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="network?.description"
        />
        <ContentBlockAttachedFiles
          v-if="network?.documentation?.length"
          id="Files"
          title="Attached Files"
          :documents="network.documentation"
        />

        <ContentBlockContact
          v-if="network?.contacts"
          id="Contributors"
          title="Contact and Contributors"
          :contributors="network?.contacts"
        />

        <!-- <ContentBlockVariables
          id="Variables"
          title="Variables &amp; Topics"
          description="Explantation about variables and the functionality seen here."
        /> -->

        <ContentBlockData
          id="AvailableData"
          title="Available Data &amp; Samples"
          :collectionEvents="network?.collectionEvents"
        />

        <ContentBlockPartners
          v-if="network?.additionalOrganisations"
          id="Partners"
          title="Partners"
          description=""
          :partners="network?.additionalOrganisations"
        />

        <ContentBlock
          id="access-conditions"
          title="Access conditions"
          :description="network?.dataAccessConditionsDescription"
          v-if="
            network?.dataAccessConditions?.length ||
            network?.dataAccessConditionsDescription ||
            network?.releaseDescription
          "
        >
          <CatalogueItemList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Citation requirements "
          v-if="network?.fundingStatement || network?.acknowledgements"
        >
          <CatalogueItemList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <TableContent
          v-if="network.cohorts_agg?.count > 0"
          id="cohorts"
          title="Cohorts"
          description="Cohort studies connected in this network"
          :headers="[
            { id: 'name', label: 'Name', singleLine: true },
            { id: 'design', label: 'Design' },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          type="Cohorts"
          :query="cohortsQuery"
          :filter="{ id: route.params.network }"
          :rowMapper="cohortMapper"
          v-slot="slotProps"
        >
          {{ slotProps }}
        </TableContent>

        <TableContent
          v-if="network.dataSources_agg?.count > 0"
          id="datasources"
          title="Data sources"
          description="Datasources connected in this network"
          :headers="[
            { id: 'name', label: 'Name', singleLine: true },
            { id: 'type', label: 'type' },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          type="DataSources"
          :query="datasourcesQuery"
          :filter="{ id: network.id }"
          :rowMapper="dataSourcesMapper"
          v-slot="slotProps"
        >
          {{ slotProps }}
        </TableContent>

        <TableContent
          v-if="networkVariablesCount > 0"
          id="variables"
          title="Variables"
          description="Variables in this network."
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'label', label: 'Label' },
            { id: 'model', label: 'Model' },
          ]"
          type="Variables"
          :query="variablesQuery"
          :filter="networkVariablesFilter"
          :rowMapper="variableMapper"
        >
          <ContentBlockModal
            title="Variables"
            description="Under construction"
          ></ContentBlockModal>
        </TableContent>
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
