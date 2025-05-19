<script setup lang="ts">
import { gql } from "graphql-request";
import subpopulationsQuery from "../../../../../../gql/subpopulations";
import collectionEventsQuery from "../../../../../../gql/collectionEvents";
import datasetQuery from "../../../../../../gql/datasets";
import ontologyFragment from "../../../../../../gql/fragments/ontology";
import fileFragment from "../../../../../../gql/fragments/file";
import variablesQuery from "../../../../../../gql/variables";
import { getKey } from "../../../../../../utils/variableUtils";
import { resourceIdPath } from "../../../../../../utils/urlHelpers";
import type {
  IDefinitionListItem,
  IMgError,
  IOntologyItem,
  linkTarget,
  DefinitionListItemType,
  IVariable,
} from "../../../../../../interfaces/types";
import dateUtils from "../../../../../../utils/dateUtils";
import type { IResources } from "../../../../../../interfaces/catalogue";
import { useRuntimeConfig, useRoute, useFetch, useHead } from "#app";
import {
  moduleToString,
  logError,
  removeChildIfParentSelected,
} from "#imports";
import { computed, ref } from "vue";
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
      dateLastRefresh
      startYear
      endYear
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
      design {
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
      exclusionCriteria ${moduleToString(ontologyFragment)}
      otherExclusionCriteria
      publications(orderby: {title:ASC}) {
        doi
        title
        isDesignPublication
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
        subpopulations {
          name
        }
        coreVariables
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
      organisationsInvolved(orderby: {name: ASC})  {
        id
        name
        website
        acronym
        isLeadOrganisation
        role ${moduleToString(ontologyFragment)}
        country ${moduleToString(ontologyFragment)}
      }
      subpopulations {
          name
          mainMedicalCondition ${moduleToString(ontologyFragment)}
      }
      dataAccessConditions ${moduleToString(ontologyFragment)}
      dataAccessConditionsDescription
      dataUseConditions ${moduleToString(ontologyFragment)}
      dataAccessFee
      releaseType ${moduleToString(ontologyFragment)}
      releaseDescription
      fundingStatement
      acknowledgements
      linkageOptions
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
      partOfResources {
        id
        name
        type {
            name
        }
        website
        logo {
          url
        }
      }
      publications_agg {
        count
      }
      subpopulations_agg {
        count
      }
      collectionEvents_agg{
        count
      }
    }
    Variables_agg(filter: { resource: { id: { equals: [$id] } } }) {
      count
    }
  }
`;
const variables = { id: route.params.resource };
interface IResourceQueryResponseValue extends IResources {
  publications_agg: { count: number };
  subpopulations_agg: { count: number };
  collectionEvents_agg: { count: number };
}
interface IResponse {
  data: {
    Resources: IResourceQueryResponseValue[];
    Variables_agg: { count: number };
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
  logError(error.value, "Error fetching resource metadata");
}

const resource = computed(
  () => data.value?.data?.Resources[0] as IResourceQueryResponseValue
);
const subpopulations = computed(() => resource.value.subpopulations as any[]);
const mainMedicalConditions = computed(() => {
  if (!subpopulations.value || !subpopulations.value.length) {
    return [];
  } else {
    const allItems = subpopulations.value
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
const subpopulationCount = computed(
  () => resource.value.subpopulations_agg?.count
);

const variableCount = computed(() => data.value?.data?.Variables_agg.count);

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
    _path: `/${route.params.schema}/catalogue/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}/collection-events/${item.name}`,
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

function subpopulationMapper(subpopulation: any) {
  return {
    id: subpopulation.name,
    name: subpopulation.name,
    description: subpopulation.description,
    numberOfParticipants: subpopulation.numberOfParticipants,
    _renderComponent: "SubpopulationDisplay",
    _path: `/${route.params.schema}/catalogue/${route.params.catalogue}/${route.params.resourceType}/${route.params.resource}/subpopulations/${subpopulation.name}`,
  };
}

function variableMapper(variable: IVariable) {
  const key = getKey(variable);

  return {
    id: key,
    name: variable.name,
    dataset: variable.dataset.name,
    _renderComponent: "VariableDisplay",
    _path: `/${route.params.schema}/catalogue/${route.params.catalogue}/${
      route.params.resourceType
    }/${route.params.resource}/variables/${variable.name}${resourceIdPath(
      key
    )}`,
  };
}

const datasetOptions = ref<Array<{ name: string }>>([{ name: "All datasets" }]);
const datasetFilter = ref<string>("All datasets");
const variableSearchValue = ref<string>("");

const variablesFilter = computed(() => {
  return {
    filter: {
      resource: { id: { equals: route.params.resource } },
      dataset:
        datasetFilter.value === "All datasets"
          ? undefined
          : { name: { equals: datasetFilter.value } },
    },
  };
});

async function fetchDatasetOptions() {
  const query = gql`
    query DatasetOptions($id: String) {
      Datasets(filter: { resource: { id: { equals: [$id] } } }) {
        name
      }
    }
  `;
  const variables = { id: route.params.resource };
  const { data, error } = await useFetch<
    { data: { Datasets: { name: string }[] } },
    IMgError
  >(`/${route.params.schema}/graphql`, {
    method: "POST",
    body: { query, variables },
  });

  if (error.value) {
    logError(error.value, "Error fetching dataset options");
  }

  datasetOptions.value = data.value?.data?.Datasets
    ? [...datasetOptions.value, ...data.value?.data?.Datasets]
    : datasetOptions.value;
}

fetchDatasetOptions();

const networks = computed(() =>
  !resource.value.partOfResources
    ? []
    : resource.value.partOfResources.filter((c) =>
        c.type.find((t) => t.name == "Network")
      )
);

const tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
  if (showPopulation.value) {
    tableOffContents.push({
      label: "Population",
      id: "population",
    });
  }
  if (organisations.value) {
    tableOffContents.push({
      label: "Organisations",
      id: "Organisations",
    });
  }
  if (peopleInvolvedSortedByRoleAndName.value.length > 0) {
    tableOffContents.push({
      label: "Contributors",
      id: "Contributors",
    });
  }

  if (variableCount.value ?? 0 > 0) {
    tableOffContents.push({ label: "Dataset variables", id: "DataVariables" });
  } else if (resource.value.datasets?.length) {
    tableOffContents.push({ label: "Datasets", id: "Datasets" });
  }

  if (subpopulationCount.value ?? 0 > 0) {
    tableOffContents.push({
      label: "Subpopulations",
      id: "Subpopulations",
    });
  }

  if (collectionEventCount.value ?? 0 > 0) {
    tableOffContents.push({
      label: "Collection events",
      id: "CollectionEvents",
    });
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
          .sort((a, b) => (b.order ?? 0) - (a.order ?? 0))
          .map((country) => country.name)
          .join(", ")
      : undefined,
  },
  {
    label: "Regions",
    content: resource.value?.regions
      ?.sort((a, b) => (b.order ?? 0) - (a.order ?? 0))
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
  {
    label: "Exclusion criteria",
    type: "ONTOLOGY",
    content: resource.value.exclusionCriteria,
  },
  {
    label: "Other exclusion criteria",
    content: resource.value.otherExclusionCriteria,
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
      type: "ONTOLOGY" as DefinitionListItemType,
      content: resource.value.dataAccessConditions,
    });
  }
  if (resource.value.dataUseConditions) {
    items.push({
      label: "Data use conditions",
      type: "ONTOLOGY" as DefinitionListItemType,
      content: resource.value.dataUseConditions,
    });
  }
  if (resource.value.dataAccessFee !== undefined) {
    items.push({
      label: "Data access fee",
      content: resource.value.dataAccessFee,
    });
  }
  if (resource.value.releaseType !== undefined) {
    items.push({
      label: "Release type",
      type: "ONTOLOGY" as DefinitionListItemType,
      content: resource.value.releaseType,
    });
  }
  if (resource.value.releaseDescription) {
    items.push({
      label: "Release description",
      content: resource.value.releaseDescription,
    });
  }
  if (resource.value.prelinked !== undefined) {
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

useHead({
  title: resource.value.acronym || resource.value.name,
  meta: [{ name: "description", content: resource.value.description }],
});

const messageFilter = `{"filter": {"id":{"equals":"${route.params.resource}"}}}`;

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting === "true" || config.public.cohortOnly;
});

const crumbs: any = {};
if (route.params.catalogue) {
  crumbs[
    cohortOnly.value ? "home" : (route.params.catalogue as string)
  ] = `/${route.params.schema}/catalogue/${route.params.catalogue}`;
  if (route.params.resourceType !== "about")
    crumbs[
      route.params.resourceType as string
    ] = `/${route.params.schema}/catalogue/${route.params.catalogue}/${route.params.resourceType}`;
  crumbs[route.params.resource as string] = "";
} else {
  crumbs["Home"] = `/${route.params.schema}/catalogue/`;
  crumbs["Browse"] = `/${route.params.schema}/catalogue/all`;
  if (route.params.resourceType !== "about")
    crumbs[
      route.params.resourceType as string
    ] = `/${route.params.schema}/catalogue/all/${route.params.resourceType}`;
}

const peopleInvolvedSortedByRoleAndName = computed(() =>
  [...(resource.value.peopleInvolved ?? [])].sort((a, b) => {
    const minimumOrderOfRolesA = a.role?.length
      ? Math.min(...a.role?.map((role) => role.order ?? Infinity))
      : Infinity;
    const minimumOrderOfRolesB = b.role?.length
      ? Math.min(...b.role?.map((role) => role.order ?? Infinity))
      : Infinity;
    if (minimumOrderOfRolesA !== minimumOrderOfRolesB) {
      return minimumOrderOfRolesA - minimumOrderOfRolesB;
    } else if (a.lastName !== b.lastName) {
      return a.lastName.localeCompare(b.lastName);
    } else {
      return a.firstName.localeCompare(b.firstName);
    }
  })
);
const organisations = computed(() => resource.value.organisationsInvolved);
const showPopulation = computed(
  () =>
    !!population.filter(
      (item) => item.content !== undefined && item.content !== ""
    ).length
);
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        id="resource-page-header"
        :title="
          route.params.resourceType === 'about'
            ? 'About '
            : resource?.acronym || resource.name
        "
        :description="
          (route.params.resourceType === 'about' ? 'About ' : '') +
          (resource?.name ? resource.name : '')
        "
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
        header-target="#resource-page-header"
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
          :resource="resource as IResources"
        />

        <ContentBlock v-if="showPopulation" id="population" title="Population">
          <CatalogueItemList :items="population" />
        </ContentBlock>

        <ContentBlockOrganisations
          v-if="organisations"
          id="Organisations"
          title="Organisations"
          :organisations="organisations"
        ></ContentBlockOrganisations>

        <ContentBlockContact
          v-if="peopleInvolvedSortedByRoleAndName.length > 0"
          id="Contributors"
          title="Contributors"
          :contributors="peopleInvolvedSortedByRoleAndName"
        >
        </ContentBlockContact>

        <TableContent
          v-if="resource.datasets && !variableCount"
          id="Datasets"
          title="Datasets"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
          ]"
          type="Datasets"
          :query="datasetQuery"
          :filter="{ id: route.params.resource }"
          :rowMapper="datasetMapper"
          v-slot="slotProps"
        >
          <DatasetDisplay
            :name="slotProps.id.name"
            :resource-id="slotProps.id.resource"
          />
        </TableContent>

        <ContentBlock
          title="Dataset variables"
          id="DataVariables"
          v-if="variableCount ?? 0 > 0"
        >
          <TableContent
            v-if="resource.datasets"
            id="Datasets"
            title="Datasets"
            description="Datasets and their description"
            :wrapper-component="false"
            :headers="[
              { id: 'name', label: 'Name' },
              { id: 'description', label: 'Description', singleLine: true },
            ]"
            type="Datasets"
            :query="datasetQuery"
            :filter="{ id: route.params.resource }"
            :rowMapper="datasetMapper"
            v-slot="slotProps"
          >
            <DatasetDisplay
              :name="slotProps.id.name"
              :resource-id="slotProps.id.resource"
            />
          </TableContent>

          <TableContent
            class="mt-11"
            :wrapper-component="false"
            title="Dataset variables"
            id="Variables"
            description="Dataset variables and their description"
            :headers="[
              { id: 'name', label: 'variable' },
              { id: 'dataset', label: 'Dataset' },
            ]"
            type="Variables"
            :query="variablesQuery"
            :filter="variablesFilter"
            :search-filter-value="variableSearchValue"
            :rowMapper="variableMapper"
          >
            <template #filter-group>
              <div class="relative">
                <label
                  class="block absolute text-body-xs top-2 left-6 pointer-events-none"
                  for="filter-by-data-set"
                >
                  Filter by dataset
                </label>
                <select
                  v-model="datasetFilter"
                  name="filter-by-data-set"
                  class="h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none"
                >
                  <option v-for="option in datasetOptions" :value="option.name">
                    {{ option.name }}
                  </option>
                </select>
                <span class="absolute right-5 top-5 pointer-events-none">
                  <BaseIcon name="caret-down" :width="20" />
                </span>
              </div>
              <div class="relative">
                <label
                  class="block absolute text-body-xs top-2 left-6 pointer-events-none"
                  for="filter-by-variable"
                >
                  Filter by variable
                </label>
                <input
                  v-model="variableSearchValue"
                  @click="variableSearchValue = ''"
                  name="filter-by-variable"
                  class="h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none"
                />
                <span class="absolute right-5 top-5 pointer-events-none">
                  <BaseIcon name="cross" class="stroke-1" :width="20" />
                </span>
              </div>
            </template>
            <template #default="slotProps">
              <VariableDisplay :variable-key="slotProps.id" />
            </template>
            <template #before-content> </template>
          </TableContent>
        </ContentBlock>

        <TableContent
          v-if="subpopulationCount ?? 0 > 0"
          id="Subpopulations"
          title="Subpopulations"
          description="List of subpopulations for this resource"
          :headers="[
            { id: 'name', label: 'Name' },
            { id: 'description', label: 'Description', singleLine: true },
            { id: 'numberOfParticipants', label: 'Number of participants' },
          ]"
          type="Subpopulations"
          :query="subpopulationsQuery"
          :filter="{ id: route.params.resource }"
          :rowMapper="subpopulationMapper"
          v-slot="slotProps"
        >
          <SubpopulationDisplay :id="slotProps.id" />
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
          :filter="{ id: route.params.resource }"
          :rowMapper="collectionEventMapper"
          v-slot="slotProps"
        >
          <CollectionEventDisplay :id="slotProps.id" />
        </TableContent>

        <ContentBlock
          v-if="networks.length"
          title="Networks"
          id="Networks"
          description="Part of networks"
        >
          <ReferenceCardList>
            <ReferenceCard
              v-for="network in networks"
              :title="network.name"
              :description="network?.description || ''"
              :imageUrl="network?.logo?.url || ''"
              :url="`/${route.params.schema}/catalogue/${route.params.catalogue}/networks/${network.id}`"
              :links="[
                  network.website ? { title: 'Website', url: network.website, target: '_blank' as linkTarget } : null,
               {title: 'Network details',
                url: `/${route.params.schema}/catalogue/${route.params.catalogue}/networks/${network.id}`,
                },
               network.type?.some( (type) => type.name === 'Catalogue')
               ? {
                title: 'Catalogue',
                url: `/${route.params.schema}/catalogue/${network.id}`,
              }: null
                ].filter((link) => link !== null)"
              target="_self"
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
