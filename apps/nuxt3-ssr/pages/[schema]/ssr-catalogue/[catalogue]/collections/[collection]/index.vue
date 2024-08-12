<script setup lang="ts">
import { gql } from "graphql-request";
import subcohortsQuery from "~~/gql/subcohorts";
import collectionEventsQuery from "~~/gql/collectionEvents";
import publicationsQuery from "~~/gql/publications";
import datasetQuery from "~~/gql/datasets";
import ontologyFragment from "~~/gql/fragments/ontology";
import fileFragment from "~~/gql/fragments/file";
import type {
  ICollection,
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
  query Collections($id: String) {
    Collections(filter: { id: { equals: [$id] } }) {
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
      }
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
        subcohorts {
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
      subcohorts {
          name
          mainMedicalCondition ${moduleToString(ontologyFragment)}
      }
      subcohorts_agg {
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
    }
  }
`;
const variables = { id: route.params.collection };
interface IResponse {
  data: {
    Collections: ICollection[];
    Publications_agg: { count: number };
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

const collection = computed(
  () => data.value?.data?.Collections[0] as ICollection
);
const subcohorts = computed(() => collection.value.subcohorts as any[]);
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
  () => collection.value.collectionEvents_agg?.count
);
const subcohortCount = computed(() => collection.value.subcohorts_agg?.count);

const publicationsCount = computed(
  () => data.value?.data?.Publications_agg?.count
);

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
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/collections/${route.params.collection}/collection-events/${item.name}`,
  };
}

function datasetMapper(item: { name: string; description?: string }) {
  return {
    id: {
      name: item.name,
      resourceId: route.params.collection,
    },
    name: item.name,
    description: item.description,
  };
}

function subcohortMapper(subcohort: any) {
  console.log(subcohort);
  return {
    id: subcohort.name,
    name: subcohort.name,
    description: subcohort.description,
    numberOfParticipants: subcohort.numberOfParticipants,
    _renderComponent: "SubCohortDisplay",
    _path: `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/collections/${route.params.collection}/subcohorts/${subcohort.name}`,
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
  if (collection.value.peopleInvolved) {
    tableOffContents.push({
      label: "Contributors",
      id: "Contributors",
    });
  }
  if (collection.value.collectionEvents) {
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
  if (collection.value.datasets) {
    tableOffContents.push({ label: "Datasets", id: "Datasets" });
  }

  if (collection.value.networks) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }

  if (collection.value.publications) {
    tableOffContents.push({ label: "Publications", id: "publications" });
  }

  if (
    collection.value.dataAccessConditions?.length ||
    collection.value.dataAccessConditionsDescription ||
    collection.value.releaseDescription ||
    collection.value.linkageOptions
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (collection.value.fundingStatement || collection.value.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Acknowledgements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (collection.value.documentation) {
    tableOffContents.push({ label: "Documentation", id: "Files" });
  }

  return tableOffContents;
});

const population: IDefinitionListItem[] = [
  {
    label: "Countries",
    content: collection.value?.countries
      ? [...collection.value?.countries]
          .sort((a, b) => b.order - a.order)
          .map((country) => country.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Regions",
    content: collection.value?.regions
      ?.sort((a, b) => b.order - a.order)
      .map((r) => r.name)
      .join(", "),
  },
  {
    label: "Number of participants",
    content: collection.value?.numberOfParticipants,
  },
  {
    label: "Number of participants with samples",
    content: collection.value?.numberOfParticipantsWithSamples,
  },
  {
    label: "Population age groups",
    content: removeChildIfParentSelected(
      collection.value?.populationAgeGroups || []
    )
      .sort((a, b) => a.order - b.order)
      .map((ageGroup) => ageGroup.name)
      .join(", "),
  },
  {
    label: "Population oncology topology",
    type: "ONTOLOGY",
    content: collection.value.populationOncologyTopology,
  },
  {
    label: "Population oncology morphology",
    type: "ONTOLOGY",
    content: collection.value.populationOncologyMorphology,
  },
  {
    label: "Inclusion criteria",
    type: "ONTOLOGY",
    content: collection.value.inclusionCriteria,
  },
  {
    label: "Other inclusion criteria",
    content: collection.value.otherInclusionCriteria,
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
  if (collection.value.dataAccessConditions?.length) {
    items.push({
      label: "Data access conditions",
      content: collection.value.dataAccessConditions.map((c) => c.name),
    });
  }
  if (collection.value.dataUseConditions) {
    items.push({
      label: "Data use conditions",
      type: "ONTOLOGY",
      content: collection.value.dataUseConditions,
    });
  }
  if (collection.value.dataAccessFee) {
    items.push({
      label: "Data access fee",
      content: collection.value.dataAccessFee,
    });
  }
  if (collection.value.releaseType) {
    items.push({
      label: "Release type",
      type: "ONTOLOGY",
      content: collection.value.releaseType,
    });
  }
  if (collection.value.releaseDescription) {
    items.push({
      label: "Release description",
      content: collection.value.releaseDescription,
    });
  }
  if (collection.value.prelinked) {
    items.push({
      label: "Prelinked",
      content: collection.value.prelinked,
    });
  }
  if (collection.value.linkageOptions) {
    items.push({
      label: "Linkage options",
      content: collection.value.linkageOptions,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (collection.value.fundingStatement) {
    items.push({
      label: "Funding",
      content: collection.value.fundingStatement,
    });
  }
  if (collection.value.acknowledgements) {
    items.push({
      label: "Acknowledgements",
      content: collection.value.acknowledgements,
    });
  }

  return items;
});

useHead({ title: collection.value.acronym || collection.value.name });

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
    "Collections"
  ] = `/${route.params.schema}/ssr-catalogue/${route.params.catalogue}/collections`;
} else {
  crumbs["Home"] = `/${route.params.schema}/ssr-catalogue/`;
  crumbs["Browse"] = `/${route.params.schema}/ssr-catalogue/all`;
  crumbs[
    "Collections"
  ] = `/${route.params.schema}/ssr-catalogue/all/collections`;
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
    activeAdditionalOrganisationSideModalIndex.value > -1 &&
    collection.value.organisationsInvolved
  ) {
    return collection.value.organisationsInvolved[
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
        :title="collection?.acronym || collection.name"
        :description="collection?.acronym ? collection.name : ''"
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
        :title="collection?.acronym || collection?.name"
        :image="collection?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="collection">
        <ContentBlockIntro
          :image="collection?.logo?.url"
          :link="collection?.website as linkTarget"
          :contact="collection?.contactEmail"
          :contact-name="collection.name"
          :contact-message-filter="messageFilter"
          :subject-template="collection.acronym"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="collection?.description"
        />

        <ContentCohortGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :collection="collection"
        />

        <ContentBlock id="population" title="Population">
          <CatalogueItemList
            :items="population.filter((item) => item.content !== undefined)"
          />
        </ContentBlock>

        <ContentBlockContact
          v-if="collection?.peopleInvolved || collection.organisationsInvolved"
          id="Contributors"
          title="Contributors"
          :contributors="collection?.peopleInvolved"
        >
          <template
            #before
            v-if="
              collection.organisationsInvolved &&
              collection.organisationsInvolved?.length > 0
            "
          >
            <DisplayList
              class="mb-5"
              v-if="collection.organisationsInvolved"
              title="Organisations involved"
              :type="
                collection.organisationsInvolved &&
                collection.organisationsInvolved?.length > 1
                  ? 'standard'
                  : 'link'
              "
            >
              <DisplayListItem
                v-for="(
                  organisation, index
                ) in collection.organisationsInvolved"
                @click="showLeadOrganisationSideModal(index)"
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
          :collectionEvents="collection?.collectionEvents"
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
          type="CollectionSubcohorts"
          :query="subcohortsQuery"
          :filter="{ id: route.params.collection }"
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
          :filter="{ id: route.params.collection }"
          :rowMapper="collectionEventMapper"
          v-slot="slotProps"
        >
          <CollectionEventDisplay :id="slotProps.id" />
        </TableContent>

        <TableContent
          v-if="collection.datasets"
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
            :resourceId="slotProps.id.resourceId"
          />
        </TableContent>

        <ContentBlockPartners
          v-if="collection?.additionalOrganisations"
          id="Partners"
          title="Partners"
          description=""
          :partners="collection?.additionalOrganisations"
        />

        <ContentBlockNetwork
          v-if="collection?.networks"
          id="Networks"
          title="Networks"
          description="List of networks in which this collection is involved"
          :networks="collection?.networks"
        />

        <ContentBlockPublications
          v-if="collection?.publications"
          id="publications"
          title="Publications"
          :publications="collection.publications"
        >
        </ContentBlockPublications>

        <ContentBlock
          id="access-conditions"
          title="Access conditions"
          :description="collection?.dataAccessConditionsDescription"
          v-if="
            collection?.dataAccessConditions?.length ||
            collection?.dataAccessConditionsDescription ||
            collection?.releaseDescription
          "
        >
          <CatalogueItemList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Acknowledgements "
          v-if="collection?.fundingStatement || collection?.acknowledgements"
        >
          <CatalogueItemList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <ContentBlockAttachedFiles
          v-if="collection?.documentation?.length"
          id="Files"
          title="Documentation"
          :documents="collection.documentation"
        />
      </ContentBlocks>
    </template>
  </LayoutsDetailPage>
</template>
