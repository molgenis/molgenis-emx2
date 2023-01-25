<script setup lang="ts">
import { gql } from "graphql-request";
import { Ref } from "vue";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import ontologyFragment from "~~/gql/fragments/ontology";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Cohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
      acronym
      name
      description
      website
      logo {
        url
      }
      contactEmail
      institution {
        acronym
      }
      type {
        name
      }
      collectionType {
        name
      }
      populationAgeGroups ${loadGql(ontologyFragment)}
      startYear
      endYear
      countries {
        name
        order
      }
      regions {
        name
        order
      }
      numberOfParticipants
      numberOfParticipantsWithSamples
      designDescription
      design {
        definition
        name
      }
      designPaper {
        title
        doi
      }
      inclusionCriteria
      partners {
        institution {
          pid
          acronym
          name
          website
          description
          logo {
            url
          }
        }
      }
      networks {
        pid
        name
        description
        website
        logo {
          id
          url
          size
          extension
        }
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
        coreVariables {
          name
        }
      }
      contributors {
        contributionDescription
        contact {
          firstName
          surname
          initials
          department
          email
          title {
            name
          }
          institution {
            name
          }
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
      documentation {
        name
        file {
          url
        }
        url
      }
      fundingStatement
      acknowledgements
    }
  }
`;
const variables = { pid: route.params.cohort };

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

fetchGql(collectionEventsQuery, { pid: route.params.cohort })
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

fetchGql(subcohortsQuery, { pid: route.params.cohort })
  .then((resp) => onSubcohortsLoaded(resp.data.Subcohorts))
  .catch((e) => console.log(e));

let subcohorts: Ref = ref([]);
function onSubcohortsLoaded(rows: any) {
  if (!rows?.length) {
    return;
  }

  const topLevelAgeGroup = (ageGroup: { parent: any }): any => {
    if (!ageGroup.parent) {
      return ageGroup;
    }
    return topLevelAgeGroup(ageGroup.parent);
  };

  const mapped = rows.map((subcohort: any) => {
    return {
      name: subcohort.name,
      description: subcohort.description,
      numberOfParticipants: subcohort.numberOfParticipants,
      ageGroups: !subcohort.ageGroups
        ? undefined
        : subcohort?.ageGroups
            .map(topLevelAgeGroup)
            .reduce((ageGroups: any[], ageGroup: { name: string }) => {
              if (!ageGroups.find((ag) => ageGroup.name === ag.name)) {
                ageGroups.push(ageGroup);
              }
              return ageGroups;
            }, [])
            .map((ag: { name: string }) => ag.name)
            .join(", "),
      _renderComponent: "SubCohortDisplay",
      _path: `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}/subcohorts/${subcohort.name}`,
    };
  });

  subcohorts.value = mapped;
}

let tocItems = computed(() => {
  let items = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
  if (cohort?.contributors) {
    items.push({ label: "Contact & contributors", id: "Contributors" });
  }
  if (cohort?.collectionEvents) {
    items.push({ label: "Available data & samples", id: "AvailableData" });
  }
  // { label: 'Variables & topics', id: 'Variables' },
  if (subcohorts?.value?.length) {
    items.push({ label: "Subpopulations", id: "Subpopulations" });
  }
  if (collectionEvents?.value?.length)
    items.push({ label: "Collection events", id: "CollectionEvents" });
  if (cohort?.networks) {
    items.push({ label: "Networks", id: "Networks" });
  }
  if (cohort?.partners) {
    items.push({ label: "Partners", id: "Partners" });
  }

  if (
    cohort?.dataAccessConditions?.length ||
    cohort?.dataAccessConditionsDescription ||
    cohort?.releaseDescription
  ) {
    items.push({ label: "Access Conditions", id: "access-conditions" });
  }

  if (cohort?.fundingStatement || cohort?.acknowledgements) {
    items.push({
      label: "Funding & Acknowledgements",
      id: "funding-and-acknowledgement",
    });
  }

  return items;
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
      label: "Acknowledgements",
      content: cohort.acknowledgements,
    });
  }

  return items;
});
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
              // Home: `/${route.params.schema}/ssr-catalogue`,
              Cohorts: `/${route.params.schema}/ssr-catalogue`,
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
        :title="cohort.acronym"
        :image="cohort?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="cohort">
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
        <!-- <ContentBlockAttachedFiles
          id="Files"
          title="Attached Files Generic Example"
        /> -->

        <ContentBlockContact
          v-if="cohort?.contributors"
          id="Contributors"
          title="Contact and Contributors"
          :contributors="cohort?.contributors"
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
          v-if="subcohorts?.length"
          id="Subpopulations"
          title="Subpopulations"
          description="List of subcohorts or subpopulations for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Number of participants' },
            { id: 'ageGroups', label: 'Age categories' },
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
          v-if="cohort?.partners"
          id="Partners"
          title="Partners"
          description=""
          :partners="cohort?.partners"
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
          title="Funding &amp; Acknowledgement"
          v-if="cohort?.fundingStatement || cohort?.acknowledgements"
        >
          <DefinitionList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>
      </ContentBlocks> </template
    >f
  </LayoutsDetailPage>
</template>
