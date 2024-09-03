<script setup lang="ts">
import { gql } from "graphql-request";
import cohortsQuery from "~~/gql/cohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import datasetQuery from "~~/gql/datasets";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";
import type {
  IResource,
  IDefinitionListItem,
  IMgError,
  IOntologyItem,
  linkTarget,
} from "~/interfaces/types";
import dateUtils from "~/utils/dateUtils";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query Resources($id: String) {
    Resources(filter: { id: { equals: [$id] } }) {
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
      type {
        name
      }
      typeOther
      cohortType {
        name
      }
      rWDType {
        name
      }
      networkType {
        name
      }
      clinicalStudyType {
        name
      }
      keywords
      externalIdentifiers {
        identifier
        externalIdentifierType{name}
      }
      populationAgeGroups {
        name order code parent { code }
      }
      dateEstablished
      startDataCollection
      endDataCollection
      license
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
      designType {
        definition
        name
      }
      dataCollectionType {
        definition
        name
      }
      dataCollectionDescription
      reasonSustained
      unitOfObservation
      recordTrigger
      populationOncologyTopology ${moduleToString(ontologyFragment)}
      populationOncologyMorphology ${moduleToString(ontologyFragment)}
      inclusionCriteria ${moduleToString(ontologyFragment)}
      otherInclusionCriteria
      publications(orderby: {title:ASC}) {
        doi
        title
        isDesignPublication
      }
      publications_agg{count}
      collectionEvents {
        name
        description
        startDate
        endDate
        numberOfParticipants
        ageGroups ${moduleToString(ontologyFragment)}
        dataCategories ${moduleToString(ontologyFragment)}
        sampleCategories ${moduleToString(ontologyFragment)}
        areasOfInformation ${moduleToString(ontologyFragment)}
        cohorts {
          name
        }
        coreVariables
      }
      collectionEvents_agg {
         count
      }
      peopleInvolved {
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
      organisationsInvolved  {
        id
        name
        website
        acronym
        role ${moduleToString(ontologyFragment)}
        country ${moduleToString(ontologyFragment)}
      }
      cohorts {
          name
          mainMedicalCondition ${moduleToString(ontologyFragment)}
      }
      cohorts_agg {
            count
      }
      dataAccessConditions ${moduleToString(ontologyFragment)}
      dataAccessConditionsDescription
      dataUseConditions ${moduleToString(ontologyFragment)}
      dataAccessFee
      releaseType ${moduleToString(ontologyFragment)}
      releaseDescription
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
      collectionEvents_agg{
        count
      }
      publications_agg {
        count
      }
      partOfCollections {
        name
        type {
            name
        }
        website
      }
    }
  }
`;
const variables = { id: route.params.collection };
interface IResponse {
  data: {
    Resources: IResource[];
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
  logError(error.value, "Error fetching collection metadata");
}

const resource = computed(() => data.value?.data?.Resources[0] as IResource);
const cohorts = computed(() => resource.value.cohorts as any[]);
const mainMedicalConditions = computed(() => {
  if (!cohorts.value || !cohorts.value.length) {
    return [];
  } else {
    const allItems = cohorts.value
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
  () => resource.value.collectionEvents_agg?.count
);
const cohortCount = computed(() => resource.value.cohorts_agg?.count);

function collectionEventMapper(item: any) {
  return {
    id: item.name,
    name: item.name,
    description: item.description,
    startAndEndYear: (() => {
      return dateUtils.startEndYear(item.startDate, item.endDate);
    })(),
    numberOfParticipants: item.numberOfParticipants,
    _renderComponent: "CollectionEventDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/resources/${route.params.collection}/collection-events/${item.name}`,
  };
}

function datasetMapper(item: { name: string; description?: string }) {
  return {
    id: {
      name: item.name,
      resource: route.params.resource,
    },
    name: item.name,
    description: item.description,
  };
}

function cohortMapper(cohort: any) {
  return {
    id: cohort.name,
    name: cohort.name,
    description: cohort.description,
    numberOfParticipants: cohort.numberOfParticipants,
    _renderComponent: "CohortDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/resources/${route.params.resource}/cohorts/${cohort.name}`,
  };
}

const networks = computed(() =>
  resource.value.partOfResources?.filter((c) =>
    c.type?.find((t) => t.name == "Network")
  )
);

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
  if (resource.value.peopleInvolved) {
    tableOffContents.push({
      label: "Contributors",
      id: "Contributors",
    });
  }
  if (resource.value.resource?.collectionEvents) {
    tableOffContents.push({
      label: "Available data & samples",
      id: "AvailableData",
    });
  }
  // { label: 'Variables & topics', id: 'Variables' },
  if (cohortCount.value ?? 0 > 0) {
    tableOffContents.push({ label: "Subpopulations", id: "Subpopulations" });
  }
  if (collectionEventCount.value ?? 0 > 0) {
    tableOffContents.push({
      label: "Collection events",
      id: "CollectionEvents",
    });
  }
  if (resource.value.datasets) {
    tableOffContents.push({ label: "Datasets", id: "Datasets" });
  }

  if (networks.value.length > 0) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }

  if (resource.value.publications) {
    tableOffContents.push({ label: "Publications", id: "publications" });
  }

  if (
    resource.value.dataAccessConditions?.length ||
    resource.value.dataAccessConditionsDescription ||
    resource.value.releaseDescription ||
    resource.value.linkageOptions
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (resource.value.fundingStatement || resource.value.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Acknowledgements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (resource.value.documentation) {
    tableOffContents.push({ label: "Documentation", id: "Files" });
  }

  return tableOffContents;
});

const population: IDefinitionListItem[] = [
  {
    label: "Countries",
    content: resource.value?.countries
      ? [...resource.value?.countries]
          .sort((a, b) => b.order - a.order)
          .map((country) => country.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Regions",
    content: resource.value?.regions
      ?.sort((a, b) => b.order - a.order)
      .map((r) => r.name)
      .join(", "),
  },
  {
    label: "Number of participants",
    content: resource.value?.numberOfParticipants,
  },
  {
    label: "Number of participants with samples",
    content: resource.value?.numberOfParticipantsWithSamples,
  },
  {
    label: "Population age groups",
    content: removeChildIfParentSelected(
      resource.value?.populationAgeGroups || []
    )
      .sort((a, b) => a.order - b.order)
      .map((ageGroup) => ageGroup.name)
      .join(", "),
  },
  {
    label: "Population oncology topology",
    type: "ONTOLOGY",
    content: resource.value.populationOncologyTopology,
  },
  {
    label: "Population oncology morphology",
    type: "ONTOLOGY",
    content: resource.value.populationOncologyMorphology,
  },
  {
    label: "Inclusion criteria",
    type: "ONTOLOGY",
    content: resource.value.inclusionCriteria,
  },
  {
    label: "Other inclusion criteria",
    content: resource.value.otherInclusionCriteria,
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
  if (resource.value.dataAccessConditions?.length) {
    items.push({
      label: "Data access conditions",
      content: resource.value.dataAccessConditions.map((c) => c.name),
    });
  }
  if (resource.value.dataUseConditions) {
    items.push({
      label: "Data use conditions",
      type: "ONTOLOGY",
      content: resource.value.dataUseConditions,
    });
  }
  if (resource.value.dataAccessFee) {
    items.push({
      label: "Data access fee",
      content: resource.value.dataAccessFee,
    });
  }
  if (resource.value.releaseType) {
    items.push({
      label: "Release type",
      type: "ONTOLOGY",
      content: resource.value.releaseType,
    });
  }
  if (resource.value.releaseDescription) {
    items.push({
      label: "Release description",
      content: resource.value.releaseDescription,
    });
  }
  if (resource.value.prelinked) {
    items.push({
      label: "Prelinked",
      content: resource.value.prelinked,
    });
  }
  if (resource.value.linkageOptions) {
    items.push({
      label: "Linkage options",
      content: resource.value.linkageOptions,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (resource.value.fundingStatement) {
    items.push({
      label: "Funding",
      content: resource.value.fundingStatement,
    });
  }
  if (resource.value.acknowledgements) {
    items.push({
      label: "Acknowledgements",
      content: resource.value.acknowledgements,
    });
  }

  return items;
});

useHead({ title: resource.value.acronym || resource.value.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.collection}"}}}`;

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
    "Resources"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/resource`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue/`;
  crumbs["Browse"] = `/${route.params.schema}/ssr-catalogue/all`;
  crumbs["Resources"] = `/${route.params.schema}/ssr-catalogue/all/resource`;
}

const activeOrganisationSideModalIndex = ref(-1);
function showOrganisationSideModal(index: number) {
  activeOrganisationSideModalIndex.value = index;
}

function closeOrganisationSideModal() {
  activeOrganisationSideModalIndex.value = -1;
}

const organisations = computed(() =>
  resource.value.organisationsInvolved?.sort((a, b) =>
    a.role && a.role?.find((r) => r.name === "Lead") ? -1 : 1
  )
);

const activeOrganization = computed(() => {
  if (activeOrganisationSideModalIndex.value > -1 && organisations.value) {
    return organisations.value[activeOrganisationSideModalIndex.value];
  } else {
    return null;
  }
});
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="resource?.acronym || resource.name"
        :description="resource?.acronym ? resource.name : ''"
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
        :title="resource?.acronym || resource?.name"
        :image="resource?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="resource">
        <ContentBlockIntro
          :image="resource?.logo?.url"
          :link="resource?.website as linkTarget"
          :contact="resource?.contactEmail"
          :contact-name="resource.name"
          :contact-message-filter="messageFilter"
          :subject-template="resource.acronym"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="resource?.description"
        />

        <ContentCohortGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :resource="resource"
        />

        <ContentBlock id="population" title="Population">
          <CatalogueItemList
            :items="population.filter((item) => item.content !== undefined)"
          />
        </ContentBlock>

        <ContentBlockContact
          v-if="resource?.peopleInvolved || organisations"
          id="Contributors"
          title="Contributors"
          :contributors="resource?.peopleInvolved"
        >
          <template #before v-if="organisations && organisations?.length > 0">
            <DisplayList
              class="mb-5"
              v-if="organisations"
              title="Organisations involved"
              :type="
                organisations && organisations.length > 1 ? 'standard' : 'link'
              "
            >
              <DisplayListItem
                v-for="(organisation, index) in organisations"
                @click="showOrganisationSideModal(index)"
              >
                <span class="text-blue-500 hover:underline hover:cursor-pointer"
                  >{{ organisation.name }}
                  <template v-if="organisation.role"
                    >({{
                      organisation.role.map((r) => r.name).join(", ")
                    }})</template
                  ></span
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
          :collectionEvents="resource?.collectionEvents"
        />

        <TableContent
          v-if="cohortCount ?? 0 > 0"
          id="Subpopulations"
          title="Subpopulations"
          description="List of subcohorts or subpopulations for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          type="CollectionSubcohorts"
          :query="cohortsQuery"
          :filter="{ id: route.params.collection }"
          :rowMapper="cohortMapper"
          v-slot="slotProps"
        >
          <CohortDisplay :id="slotProps.id" />
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
          :filter="{ id: route.params.collection }"
          :rowMapper="collectionEventMapper"
          v-slot="slotProps"
        >
          <CollectionEventDisplay :id="slotProps.id" />
        </TableContent>

        <TableContent
          v-if="resource.datasets"
          id="Datasets"
          title="Datasets"
          description="List of datasets for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
          ]"
          type="CollectionDatasets"
          :query="datasetQuery"
          :filter="{ id: route.params.collection }"
          :rowMapper="datasetMapper"
          v-slot="slotProps"
        >
          <DatasetDisplay
            :name="slotProps.id.name"
            :collectionId="slotProps.id.collection"
          />
        </TableContent>

        <ContentBlock title="Part of networks" id="Networks">
          <ReferenceCardList>
            <ReferenceCard
              v-for="network in networks"
              :title="network.name"
              :description="network?.description"
              :links="
                network.website
                  ? [
                      {
                        title: 'Website',
                        url: network.website,
                        target: '_blank',
                      },
                    ]
                  : undefined
              "
            />
          </ReferenceCardList>
        </ContentBlock>

        <ContentBlockPublications
          v-if="resource?.publications"
          id="publications"
          title="Publications"
          :publications="resource.publications"
        >
        </ContentBlockPublications>

        <ContentBlock
          id="access-conditions"
          title="Access conditions"
          :description="resource?.dataAccessConditionsDescription"
          v-if="
            resource?.dataAccessConditions?.length ||
            resource?.dataAccessConditionsDescription ||
            resource?.releaseDescription
          "
        >
          <CatalogueItemList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Acknowledgements "
          v-if="resource?.fundingStatement || resource?.acknowledgements"
        >
          <CatalogueItemList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <ContentBlockAttachedFiles
          v-if="resource?.documentation?.length"
          id="Files"
          title="Documentation"
          :documents="resource.documentation"
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
