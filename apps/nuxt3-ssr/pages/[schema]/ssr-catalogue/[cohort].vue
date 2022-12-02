<script setup lang="ts">
import { gql } from "graphql-request";
import { Ref } from "vue";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Cohorts($pid: String) {
    Cohorts(filter: { pid: { equals: [$pid] } }) {
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
      populationAgeGroups {
        name
      }
      startYear
      endYear
      countries {
        name
        order
      }
      numberOfParticipants
      designDescription
      design {
        definition
        name
      }
      partners {
        institution {
          pid
          name
          description
          logo {
            url
          }
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
        ageGroups {
          name
        }
        dataCategories {
          name
          definition
          parent {
            name
            definition
          }
        }
        sampleCategories {
          name
          definition
          parent {
            name
            definition
          }
        }
        areasOfInformation {
          name
          definition
          parent {
            name
            definition
          }
        }
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
    }
  }
`;
const variables = { pid: route.params.cohort };

let cohort: ICohort;

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

fetchGql(subcohortsQuery, { pid: route.params.cohort })
  .then(resp => onSubcohortsLoaded(resp.data.Cohorts[0].subcohorts))
  .catch(e => console.log(e))

fetchGql(collectionEventsQuery, { pid: route.params.cohort })
  .then(resp => onCollectionEventsLoaded(resp.data.Cohorts[0].collectionEvents))
  .catch(e => console.log(e))

let collectionEvents: Ref = ref([])
function onCollectionEventsLoaded(rows: any) {
  collectionEvents.value = rows.map((item: any) => {
    return {
      name: item.name,
      description: item.description,
      startAndEndYear: (() => {
        const startYear =
          item.startYear && item.startYear.name
            ? item.startYear.name
            : null;
        const endYear =
          item.endYear && item.endYear.name ? item.endYear.name : null;
        return filters.startEndYear(startYear, endYear);
      })(),
      _path: `${route.params.schema} /${route.params.cohort}/collection-event-${item.name}`,
    };
  });
}

let subcohorts: Ref = ref([])
function onSubcohortsLoaded(rows: any) {

  const topLevelAgeGroup = (ageGroup: { parent: any; }):any => {
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
      ageGroups: subcohort?.ageGroups
        .map(topLevelAgeGroup)
        .reduce((ageGroups: any[], ageGroup: { name: string; }) => {
          if (!ageGroups.find((ag) => ageGroup.name === ag.name)) {
            ageGroups.push(ageGroup);
          }
          return ageGroups;
        }, [])
        .map((ag: { name: string; }) => ag.name)
        .join(","),
      _path: `${route.params.schema}/${route.params.cohort}/subcohort-${subcohort.name}`
    }
  })
  
  subcohorts.value = mapped
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
        <!-- <ContentBlockAttachedFiles
          id="Files"
          title="Attached Files Generic Example"
        /> -->
        <ContentBlockContact
          id="Contributors"
          title="Contact and Contributors"
          :contributors="cohort?.contributors"
        />
        <!-- <ContentBlockVariables
          id="Variables"
          title="Variables & Topics"
          description="Explantation about variables and the functionality seen here."
        /> -->
        <ContentBlockData
          id="AvailableData"
          title="Available Data & Samples"
          description=""
          :collectionEvents="cohort?.collectionEvents"
        />
        <TableContent 
          title="Subpopulations" 
          description="List of subcohorts or subpopulations for this resource" 
          :headers="[
                    { id: 'name', label: 'Name' },
                    { id: 'description', label: 'Description' },
                    { id: 'numberOfParticipants', label: 'Number of participants' },
                    { id: 'ageGroups', label: 'Age categories' }
                    ]"
          :rows="subcohorts" />

        <TableContent 
          title="Collection events" 
          description="List of collection events defined for this resource" 
          :headers="
                  [
                    { id: 'name', label: 'Name' },
                    { id: 'description', label: 'Description' },
                    { id: 'startAndEndYear', label: 'Start end year' },
                  ]"
          :rows="collectionEvents" />

        <ContentBlockPartners
          id="Partners"
          title="Partners"
          description=""
          :partners="cohort?.partners"
        />
        <ContentBlockNetwork
          id="Networks"
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here."
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
