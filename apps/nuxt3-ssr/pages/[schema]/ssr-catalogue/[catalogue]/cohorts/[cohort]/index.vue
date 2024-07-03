<script setup lang="ts">
import { gql } from "graphql-request";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import publicationsQuery from "~~/gql/publications";
import datasetQuery from "~~/gql/datasets";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";
import type {
  ICohort,
  IDefinitionListItem,
  IMgError,
  IOntologyItem,
  IOrganisation,
  IPublication,
  linkTarget,
} from "~/interfaces/types";
import dateUtils from "~/utils/dateUtils";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Cohorts($id: String) {
    Cohorts(filter: { id: { equals: [$id] } }) {
      id
      pid
      acronym
      name
      description
      website
      logo {
        url
      }
      contactEmail
      leadOrganisation {
        id
        name
        email
        description
        website
        acronym
        type {
          name
        }
        institution
        institutionAcronym
        typeOther
        address
        expertise
        country {
          name
        }
        logo ${moduleToString(fileFragment)}
      }
      additionalOrganisations  {
         id
        name
        email
        description
        website
        acronym
        type {
          name
        }
        institution
        institutionAcronym
        typeOther
        address
        expertise
        country {
          name
        }
        logo ${moduleToString(fileFragment)}
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
      designSchematic ${moduleToString(fileFragment)}
      design {
        definition
        name
      }
      designPaper {
        title
        doi
      }
      populationOncologyTopology ${moduleToString(ontologyFragment)}
      populationOncologyMorphology ${moduleToString(ontologyFragment)}
      inclusionCriteria ${moduleToString(ontologyFragment)}
      otherInclusionCriteria
      additionalOrganisations {
        id
        acronym
        name
        website
        description
        logo ${moduleToString(fileFragment)}
      }
      networks {
        id
        name
        description
        website
        logo ${moduleToString(fileFragment)}
      }
      publications(orderby: {title:ASC}) {
        doi
        title
        year
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
        ageGroups ${moduleToString(ontologyFragment)}
        dataCategories ${moduleToString(ontologyFragment)}
        sampleCategories ${moduleToString(ontologyFragment)}
        areasOfInformation ${moduleToString(ontologyFragment)}
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
        role ${moduleToString(ontologyFragment)}
        
      }
      dataAccessConditions ${moduleToString(ontologyFragment)}
      dataAccessConditionsDescription
      dataUseConditions ${moduleToString(ontologyFragment)}
      dataAccessFee
      releaseType ${moduleToString(ontologyFragment)}
      releaseDescription
      linkageOptions
      fundingStatement
      acknowledgements
      prelinked
      documentation {
        name
        description
        url
        file ${moduleToString(fileFragment)}
      }
      datasets {
        name
      }
    }
     Subcohorts(
      filter: { resource: { id: { equals: [$id] } },  }
    ) {
      name
      mainMedicalCondition ${moduleToString(ontologyFragment)}
    }
    CollectionEvents_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
    Subcohorts_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
    Publications_agg(filter: { resources: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
const variables = { id: route.params.cohort };
interface IResponse {
  data: {
    Cohorts: ICohort[];
    Subcohorts: any[];
    CollectionEvents_agg: { count: number };
    Publications_agg: { count: number };
    Subcohorts_agg: { count: number };
  };
}
const { data, error } = await useFetch<IResponse, IMgError>(
  `/${route.params.schema}/graphql`,
  {
    method: "POST",
    body: { query, variables },
  }
);

if (error.value) {
  logError(error.value, "Error fetching cohort data");
}

const cohort = computed(() => data.value?.data?.Cohorts[0] as ICohort);
const subcohorts = computed(() => data.value?.data?.Subcohorts as any[]);
const mainMedicalConditions = computed(() => {
  if (!subcohorts.value || !subcohorts.value.length) {
    return [];
  } else {
    const allItems = subcohorts.value
      .map((s: { mainMedicalCondition?: IOntologyItem[] }) => {
        const combinedItems = s.mainMedicalCondition
          ? s.mainMedicalCondition
          : [];

        return combinedItems as IOntologyItem[];
      })
      .flat();

    const uniqueItems = [...new Map(allItems.map((v) => [v.name, v])).values()];
    return uniqueItems;
  }
});

const collectionEventCount = computed(
  () => data.value?.data?.CollectionEvents_agg?.count
);
const subcohortCount = computed(() => data.value?.data?.Subcohorts_agg?.count);

const publicationsCount = computed(
  () => data.value?.data?.Publications_agg?.count
);

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
      return dateUtils.startEndYear(startYear, endYear);
    })(),
    numberOfParticipants: item.numberOfParticipants,
    _renderComponent: "CollectionEventDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts/${route.params.cohort}/collection-events/${item.name}`,
  };
}

function datasetMapper(item: { name: string; description?: string }) {
  return {
    id: {
      name: item.name,
      resourceId: route.params.cohort,
    },
    name: item.name,
    description: item.description,
  };
}

function subcohortMapper(subcohort: any) {
  return {
    id: subcohort.name,
    name: subcohort.name,
    description: subcohort.description,
    numberOfParticipants: subcohort.numberOfParticipants,
    _renderComponent: "SubCohortDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts/${route.params.cohort}/subcohorts/${subcohort.name}`,
  };
}

function publicationMapper(publication: IPublication) {
  return {
    id: publication.doi,
    doi: publication.doi,
    title: publication.title,
    year: publication.year,
    _renderComponent: "PublicationDisplay",
  };
}

let tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
  if (population) {
    tableOffContents.push({
      label: "Population",
      id: "population",
    });
  }
  if (cohort.value.contacts) {
    tableOffContents.push({
      label: "Contributors",
      id: "Contributors",
    });
  }
  if (cohort.value.collectionEvents) {
    tableOffContents.push({
      label: "Available data & samples",
      id: "AvailableData",
    });
  }
  // { label: 'Variables & topics', id: 'Variables' },
  if (subcohortCount.value ?? 0 > 0) {
    tableOffContents.push({ label: "Subpopulations", id: "Subpopulations" });
  }
  if (collectionEventCount.value ?? 0 > 0) {
    tableOffContents.push({
      label: "Collection events",
      id: "CollectionEvents",
    });
  }
  if (cohort.value.datasets) {
    tableOffContents.push({ label: "Datasets", id: "Datasets" });
  }

  if (cohort.value.additionalOrganisations) {
    tableOffContents.push({ label: "Partners", id: "Partners" });
  }

  if (cohort.value.networks) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }

  if (cohort.value.publications) {
    tableOffContents.push({ label: "Publications", id: "publications" });
  }

  if (
    cohort.value.dataAccessConditions?.length ||
    cohort.value.dataAccessConditionsDescription ||
    cohort.value.releaseDescription ||
    cohort.value.linkageOptions
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (cohort.value.fundingStatement || cohort.value.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Acknowledgements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (cohort.value.documentation) {
    tableOffContents.push({ label: "Documentation", id: "Files" });
  }

  return tableOffContents;
});

const population: IDefinitionListItem[] = [
  {
    label: "Countries",
    content: cohort.value?.countries
      ? [...cohort.value?.countries]
          .sort((a, b) => b.order - a.order)
          .map((country) => country.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Regions",
    content: cohort.value?.regions
      ?.sort((a, b) => b.order - a.order)
      .map((r) => r.name)
      .join(", "),
  },
  {
    label: "Number of participants",
    content: cohort.value?.numberOfParticipants,
  },
  {
    label: "Number of participants with samples",
    content: cohort.value?.numberOfParticipantsWithSamples,
  },
  {
    label: "Population age groups",
    content: removeChildIfParentSelected(
      cohort.value?.populationAgeGroups || []
    )
      .sort((a, b) => a.order - b.order)
      .map((ageGroup) => ageGroup.name)
      .join(", "),
  },
  {
    label: "Population oncology topology",
    type: "ONTOLOGY",
    content: cohort.value.populationOncologyTopology,
  },
  {
    label: "Population oncology morphology",
    type: "ONTOLOGY",
    content: cohort.value.populationOncologyMorphology,
  },
  {
    label: "Inclusion criteria",
    type: "ONTOLOGY",
    content: cohort.value.inclusionCriteria,
  },
  {
    label: "Other inclusion criteria",
    content: cohort.value.otherInclusionCriteria,
  },
];

if (mainMedicalConditions.value && mainMedicalConditions.value.length > 0) {
  population.splice(population.length - 4, 0, {
    label: "Main medical condition",
    content: mainMedicalConditions.value,
    type: "ONTOLOGY",
  });
}

let accessConditionsItems = computed(() => {
  let items = [];
  if (cohort.value.dataAccessConditions?.length) {
    items.push({
      label: "Data access conditions",
      content: cohort.value.dataAccessConditions.map((c) => c.name),
    });
  }
  if (cohort.value.dataUseConditions) {
    items.push({
      label: "Data use conditions",
      type: "ONTOLOGY",
      content: cohort.value.dataUseConditions,
    });
  }
  if (cohort.value.dataAccessFee) {
    items.push({
      label: "Data access fee",
      content: cohort.value.dataAccessFee,
    });
  }
  if (cohort.value.releaseType) {
    items.push({
      label: "Release type",
      type: "ONTOLOGY",
      content: cohort.value.releaseType,
    });
  }
  if (cohort.value.releaseDescription) {
    items.push({
      label: "Release description",
      content: cohort.value.releaseDescription,
    });
  }
  if (cohort.value.prelinked) {
    items.push({
      label: "Prelinked",
      content: cohort.value.prelinked,
    });
  }
  if (cohort.value.linkageOptions) {
    items.push({
      label: "Linkage options",
      content: cohort.value.linkageOptions,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (cohort.value.fundingStatement) {
    items.push({
      label: "Funding",
      content: cohort.value.fundingStatement,
    });
  }
  if (cohort.value.acknowledgements) {
    items.push({
      label: "Acknowledgements",
      content: cohort.value.acknowledgements,
    });
  }

  return items;
});

useHead({ title: cohort.value.acronym || cohort.value.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.cohort}"}}}`;

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});
const crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    cohortOnly.value ? "home" : (route.params.catalogue as string)
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}`;
  crumbs[
    "Cohorts"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/cohorts`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue/`;
  crumbs["Browse"] = `/${route.params.schema}/ssr-catalogue/all`;
  crumbs["Cohorts"] = `/${route.params.schema}/ssr-catalogue/all/cohorts`;
}

const activeLeadOrganisationSideModalIndex = ref(-1);

function showLeadOrganisationSideModal(index: number) {
  activeLeadOrganisationSideModalIndex.value = index;
}
const activeAdditionalOrganisationSideModalIndex = ref(-1);

function showAdditionaOrganisationSideModal(index: number) {
  activeAdditionalOrganisationSideModalIndex.value = index;
}

function closeOrganisationSideModal() {
  activeLeadOrganisationSideModalIndex.value = -1;
  activeAdditionalOrganisationSideModalIndex.value = -1;
}

const activeOrganization = computed(() => {
  if (
    activeLeadOrganisationSideModalIndex.value > -1 &&
    cohort.value.leadOrganisation
  ) {
    return cohort.value.leadOrganisation[
      activeLeadOrganisationSideModalIndex.value
    ];
  } else if (
    activeAdditionalOrganisationSideModalIndex.value > -1 &&
    cohort.value.additionalOrganisations
  ) {
    return cohort.value.additionalOrganisations[
      activeAdditionalOrganisationSideModalIndex.value
    ];
  } else {
    return null;
  }
});
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="cohort?.acronym || cohort.name"
        :description="cohort?.acronym ? cohort.name : ''"
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
        :title="cohort?.acronym || cohort?.name"
        :image="cohort?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="cohort">
        <ContentBlockIntro
          :image="cohort?.logo?.url"
          :link="cohort?.website as linkTarget"
          :contact="cohort?.contactEmail"
          :contact-name="cohort.name"
          :contact-message-filter="messageFilter"
          :subject-template="cohort.acronym"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="cohort?.description"
        />

        <ContentCohortGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :cohort="cohort"
        />

        <ContentBlock id="population" title="Population">
          <CatalogueItemList
            :items="population.filter((item) => item.content !== undefined)"
          />
        </ContentBlock>

        {{ activeOrganization }}

        <ContentBlockContact
          v-if="
            cohort?.contacts ||
            cohort.leadOrganisation ||
            cohort.additionalOrganisations
          "
          id="Contributors"
          title="Contributors"
          :contributors="cohort?.contacts"
        >
          <template
            #before
            v-if="
              cohort.leadOrganisation && cohort.leadOrganisation?.length > 1
            "
          >
            <DisplayList
              class="mb-5"
              v-if="cohort.leadOrganisation"
              title="Lead organisation"
              :type="
                cohort.leadOrganisation && cohort.leadOrganisation?.length > 1
                  ? 'standard'
                  : 'link'
              "
            >
              <DisplayListItem
                v-for="(organisation, index) in cohort.leadOrganisation"
                @click="showLeadOrganisationSideModal(index)"
              >
                <span
                  class="text-blue-500 hover:underline hover:cursor-pointer"
                  >{{ organisation.name }}</span
                >
                <img
                  v-if="organisation.logo"
                  class="max-h-11"
                  :src="organisation.logo.url"
                />
              </DisplayListItem>
            </DisplayList>
            <h3 class="mb-2.5 font-bold text-body-base">Contributors</h3>
          </template>

          <template #after>
            <DisplayList
              class="mt-5"
              title="Additional organisations"
              v-if="cohort.additionalOrganisations"
              :type="
                cohort.additionalOrganisations &&
                cohort.additionalOrganisations?.length > 1
                  ? 'standard'
                  : 'link'
              "
            >
              <DisplayListItem
                v-for="(organisation, index) in cohort.additionalOrganisations"
                @click="showAdditionaOrganisationSideModal(index)"
              >
                <span
                  class="text-blue-500 hover:underline hover:cursor-pointer"
                  >{{ organisation.name }}</span
                >
                <img
                  v-if="organisation.logo"
                  class="max-h-11"
                  :src="organisation.logo.url"
                />
              </DisplayListItem>
            </DisplayList>
          </template>
        </ContentBlockContact>

        <SideModal
          :show="activeOrganization !== null"
          :fullScreen="false"
          :slideInRight="true"
          @close="closeOrganisationSideModal"
          buttonAlignment="right"
        >
          <slot>
            <OrganizationSideContent
              v-if="activeOrganization"
              :organisation="activeOrganization"
            />
          </slot>
        </SideModal>

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
          v-if="subcohortCount ?? 0 > 0"
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
          v-if="collectionEventCount ?? 0 > 0"
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

        <TableContent
          v-if="cohort.datasets"
          id="Datasets"
          title="Datasets"
          description="List of datasets for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
          ]"
          type="Datasets"
          :query="datasetQuery"
          :filter="{ id: route.params.cohort }"
          :rowMapper="datasetMapper"
          v-slot="slotProps"
        >
          <DatasetDisplay
            :name="slotProps.id.name"
            :resourceId="slotProps.id.resourceId"
          />
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
          description="List of networks in which this cohort is involved"
          :networks="cohort?.networks"
        />

        <ContentBlockPublications
          v-if="cohort?.publications"
          id="publications"
          title="Publications"
          :publications="cohort.publications"
        >
        </ContentBlockPublications>

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
          <CatalogueItemList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Acknowledgements "
          v-if="cohort?.fundingStatement || cohort?.acknowledgements"
        >
          <CatalogueItemList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <ContentBlockAttachedFiles
          v-if="cohort?.documentation?.length"
          id="Files"
          title="Documentation"
          :documents="cohort.documentation"
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
