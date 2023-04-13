<script setup lang="ts">
import { gql } from "graphql-request";
import { Ref } from "vue";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Networks($id: String) {
    Networks(filter: { id: { equals: [$id] } }) {
      id
      name
      description
      website
      logo ${loadGql(fileFragment)}
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
    }
  }
`;
const variables = { id: route.params.network };

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
}

fetchGql(collectionEventsQuery, { id: route.params.cohort })
  .then((resp) => onCollectionEventsLoaded(resp.data.CollectionEvents))
  .catch((e) => console.log(e));

let collectionEvents: Ref = ref([]);
function onCollectionEventsLoaded(rows: any) {
  if (!rows?.length) {
    return;
  }
  collectionEvents.value = rows.map((item: any) => {
    return {
      name: item.name,
      description: item.description,
      startAndEndYear: (() => {
        const startYear =
          item.startYear && item.startYear.name ? item.startYear.name : null;
        const endYear =
          item.endYear && item.endYear.name ? item.endYear.name : null;
        return filters.startEndYear(startYear, endYear);
      })(),
      numberOfParticipants: item.numberOfParticipants,
      _renderComponent: "CollectionEventDisplay",
      _path: `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}/collection-events/${item.name}`,
    };
  });
}

fetchGql(subcohortsQuery, { id: route.params.cohort })
  .then((resp) => onSubcohortsLoaded(resp.data.Subcohorts))
  .catch((e) => console.log(e));

let subcohorts: Ref = ref([]);
function onSubcohortsLoaded(rows: any) {
  if (!rows?.length) {
    return;
  }

  const mapped = rows.map((subcohort: any) => {
    return {
      name: subcohort.name,
      description: subcohort.description,
      numberOfParticipants: subcohort.numberOfParticipants,
      _renderComponent: "SubCohortDisplay",
      _path: `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}/subcohorts/${subcohort.name}`,
    };
  });

  subcohorts.value = mapped;
}

let tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
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
  if (subcohorts?.value?.length) {
    tableOffContents.push({ label: "Subpopulations", id: "Subpopulations" });
  }
  if (collectionEvents?.value?.length)
    tableOffContents.push({
      label: "Collection events",
      id: "CollectionEvents",
    });
  if (network?.networks) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }
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

useHead({ title: network?.acronym || network?.name });
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="network?.acronym || network?.name"
        :description="network?.acronym ? network?.name : ''"
      >
        <template #prefix>
          <BreadCrumbs
            :crumbs="{
              Home: `/${route.params.schema}/ssr-catalogue`,
              Networks: `/${route.params.schema}/ssr-catalogue/networks`,
            }"
          />
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

        <ContentBlockGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :description="network?.designDescription"
          :cohort="network"
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

        <TableContent
          v-if="subcohorts?.length"
          id="Subpopulations"
          title="Subpopulations"
          description="List of subcohorts or subpopulations for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          :rows="subcohorts"
        />

        <TableContent
          v-if="collectionEvents?.length"
          id="CollectionEvents"
          title="Collection events"
          description="List of collection events defined for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Participants' },
            { id: 'startAndEndYear', label: 'Start end year' },
          ]"
          :rows="collectionEvents"
        />

        <ContentBlockPartners
          v-if="network?.additionalOrganisations"
          id="Partners"
          title="Partners"
          description=""
          :partners="network?.additionalOrganisations"
        />

        <ContentBlockNetwork
          v-if="network?.networks"
          id="Networks"
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here."
          :networks="network?.networks"
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
          <DefinitionList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Citation requirements "
          v-if="network?.fundingStatement || network?.acknowledgements"
        >
          <DefinitionList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>
      </ContentBlocks> </template
    >f
  </LayoutsDetailPage>
</template>
