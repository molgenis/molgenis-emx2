<script setup lang="ts">
import { gql } from "graphql-request";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Cohorts($id: String) {
    Cohorts(filter: { id: { equals: [$id] } }) {
      acronym
      name
      description
      website
      logo {
        url
      }
      contactEmail
      leadOrganisation {
        acronym
      }
      type {
        name
      }
      collectionType {
        name
      }
      populationAgeGroups {
        name order code parent { code }
      }
      startYear
      endYear
      countries {
        name order
      }
      regions {
        name
        order
      }
      numberOfParticipants
      numberOfParticipantsWithSamples
      designDescription
      designSchematic ${loadGql(fileFragment)}
      design {
        definition
        name
      }
      designPaper {
        title
        doi
      }
      inclusionCriteria ${loadGql(ontologyFragment)}
      otherInclusionCriteria
      additionalOrganisations {
        id
        acronym
        name
        website
        description
        logo ${loadGql(fileFragment)}
      }
      networks {
        id
        name
        description
        website
        logo ${loadGql(fileFragment)}
      }
      collectionEvents {
        name
        description
        startYear {
          name
        }
        endYear {
          name
        }
        numberOfParticipants
        ageGroups ${loadGql(ontologyFragment)}
        dataCategories ${loadGql(ontologyFragment)}
        sampleCategories ${loadGql(ontologyFragment)}
        areasOfInformation ${loadGql(ontologyFragment)}
        subcohorts {
          name
        }
        coreVariables
      }
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
      dataAccessConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataAccessConditionsDescription
      dataUseConditions {
        name
        ontologyTermURI
        code
        definition
      }
      dataAccessFee
      releaseDescription
      linkageOptions
      fundingStatement
      acknowledgements
      documentation {
        name
        description
        url
        file ${loadGql(fileFragment)}
      }
    }
    CollectionEvents_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
    Subcohorts_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
const variables = { id: route.params.cohort };

let cohort: ICohort;

const {
  data: cohortData,
  pending,
  error,
  refresh,
} = await useFetch(`/${route.params.schema}/catalogue/graphql`, {
  baseURL: config.public.apiBase,
  method: "POST",
  body: { query, variables },
});

watch(cohortData, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  cohort = data?.data?.Cohorts[0];
}

function collectionEventMapper(item: any) {
  return {
    id: item.name,
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
}

function subcohortMapper(subcohort: any) {
  return {
    id: subcohort.name,
    name: subcohort.name,
    description: subcohort.description,
    numberOfParticipants: subcohort.numberOfParticipants,
    _renderComponent: "SubCohortDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}/subcohorts/${subcohort.name}`,
  };
}

let tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
  if (cohort?.contacts) {
    tableOffContents.push({
      label: "Contact & contributors",
      id: "Contributors",
    });
  }
  if (cohort?.collectionEvents) {
    tableOffContents.push({
      label: "Available data & samples",
      id: "AvailableData",
    });
  }
  // { label: 'Variables & topics', id: 'Variables' },
  if (cohortData.value.data.Subcohorts_agg?.count > 0) {
    tableOffContents.push({ label: "Subpopulations", id: "Subpopulations" });
  }
  if (cohortData.value.data.CollectionEvents_agg?.count > 0) {
    tableOffContents.push({
      label: "Collection events",
      id: "CollectionEvents",
    });
  }
  if (cohort?.networks) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }
  if (cohort?.additionalOrganisations) {
    tableOffContents.push({ label: "Partners", id: "Partners" });
  }

  if (
    cohort?.dataAccessConditions?.length ||
    cohort?.dataAccessConditionsDescription ||
    cohort?.releaseDescription ||
    cohort?.linkageOptions
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (cohort?.fundingStatement || cohort?.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Citation requirements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (cohort?.documentation) {
    tableOffContents.push({ label: "Attached files", id: "Files" });
  }

  return tableOffContents;
});

let accessConditionsItems = computed(() => {
  let items = [];
  if (cohort?.dataAccessConditions?.length) {
    items.push({
      label: "Conditions",
      content: cohort.dataAccessConditions.map((c) => c.name),
    });
  }
  if (cohort?.releaseDescription) {
    items.push({
      label: "Release",
      content: cohort.releaseDescription,
    });
  }
  if (cohort?.linkageOptions) {
    items.push({
      label: "Linkage options",
      content: cohort.linkageOptions,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (cohort?.fundingStatement) {
    items.push({
      label: "Funding",
      content: cohort.fundingStatement,
    });
  }
  if (cohort?.acknowledgements) {
    items.push({
      label: "Citation requirements ",
      content: cohort.acknowledgements,
    });
  }

  return items;
});

useHead({ title: cohort?.acronym || cohort?.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.cohort}"}}}`;
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="cohort?.acronym || cohort?.name"
        :description="cohort?.acronym ? cohort?.name : ''"
      >
        <template #prefix>
          <BreadCrumbs
            :crumbs="{
              Home: `/${route.params.schema}/ssr-catalogue`,
              Cohorts: `/${route.params.schema}/ssr-catalogue/cohorts`,
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
        :title="cohort?.acronym"
        :image="cohort?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="cohort">
        <ContentBlockIntro
          :image="cohort?.logo?.url"
          :link="cohort?.website"
          :contact="cohort?.contactEmail"
          :contact-name="cohort?.name"
          :contact-message-filter="messageFilter"
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

        <ContentBlockContact
          v-if="cohort?.contacts"
          id="Contributors"
          title="Contact and Contributors"
          :contributors="cohort?.contacts"
        />

        <!-- <ContentBlockVariables
          id="Variables"
          title="Variables &amp; Topics"
          description="Explantation about variables and the functionality seen here."
        /> -->

        <ContentBlockData
          id="AvailableData"
          title="Available Data &amp; Samples"
          :collectionEvents="cohort?.collectionEvents"
        />

        <TableContent
          v-if="cohortData.data.Subcohorts_agg.count > 0"
          id="Subpopulations"
          title="Subpopulations"
          description="List of subcohorts or subpopulations for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          type="Subcohorts"
          :query="subcohortsQuery"
          :filter="{ id: route.params.cohort }"
          :rowMapper="subcohortMapper"
          v-slot="slotProps"
        >
          <SubCohortDisplay :id="slotProps.id" />
        </TableContent>

        <TableContent
          v-if="cohortData.data.CollectionEvents_agg.count > 0"
          id="CollectionEvents"
          title="Collection events"
          description="List of collection events defined for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Participants' },
            {
              id: 'startAndEndYear',
              label: 'Start end year',
              orderByColumn: 'startYear',
            },
          ]"
          type="CollectionEvents"
          :query="collectionEventsQuery"
          :filter="{ id: route.params.cohort }"
          :rowMapper="collectionEventMapper"
          v-slot="slotProps"
        >
          <CollectionEventDisplay :id="slotProps.id" />
        </TableContent>

        <ContentBlockPartners
          v-if="cohort?.additionalOrganisations"
          id="Partners"
          title="Partners"
          description=""
          :partners="cohort?.additionalOrganisations"
        />

        <ContentBlockNetwork
          v-if="cohort?.networks"
          id="Networks"
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here."
          :networks="cohort?.networks"
        />

        <ContentBlock
          id="access-conditions"
          title="Access conditions"
          :description="cohort?.dataAccessConditionsDescription"
          v-if="
            cohort?.dataAccessConditions?.length ||
            cohort?.dataAccessConditionsDescription ||
            cohort?.releaseDescription
          "
        >
          <DefinitionList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Citation requirements "
          v-if="cohort?.fundingStatement || cohort?.acknowledgements"
        >
          <DefinitionList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <ContentBlockAttachedFiles
          v-if="cohort?.documentation?.length"
          id="Files"
          title="Attached Files"
          :documents="cohort.documentation"
        />
      </ContentBlocks> </template
    >f
  </LayoutsDetailPage>
</template>
