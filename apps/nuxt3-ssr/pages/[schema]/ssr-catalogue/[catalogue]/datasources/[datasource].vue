<script setup lang="ts">
defineProps({
  datasource: { type: Object },
});

import { gql } from "graphql-tag";
const config = useRuntimeConfig();
const route = useRoute();

const query = gql`
  query DataSources($id: String) {
    DataSources(filter: { id: { equals: [$id] } }) {
      id
      acronym
      name
      description
      website
      logo {
        url
      }
      leadOrganisation {
        acronym
      }
      type {
        name
      }
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
      networks {
        id
        name
        description
        website
        logo {
          id
          size
          extension
          url
        }
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
      fundingStatement
      acknowledgements
      documentation {
        name
        description
        url
        file {
          id
          size
          extension
          url
        }
      }
    }
  }
`;

console.log("id=" + route.params.datasource);
const variables = { id: route.params.datasource };

let datasource: any;

const {
  data: datasourceData,
  pending,
  error,
  refresh,
} = await useFetch(`/${route.params.schema}/catalogue/graphql`, {
  baseURL: config.public.apiBase,
  method: "POST",
  body: { query, variables },
});

console.log(query);
console.log(datasourceData);

watch(datasourceData, setData, {
  deep: true,
  immediate: true,
});

function setData(data: any) {
  datasource = data?.data?.DataSources[0];
}

let tocItems = computed(() => {
  let tableOffContents = [
    { label: "Description", id: "Description" },
    { label: "General design", id: "GeneralDesign" },
  ];
  if (datasource?.contacts) {
    tableOffContents.push({
      label: "Contact & contributors",
      id: "Contributors",
    });
  }
  if (datasource?.networks) {
    tableOffContents.push({ label: "Networks", id: "Networks" });
  }
  if (datasource?.additionalOrganisations) {
    tableOffContents.push({ label: "Partners", id: "Partners" });
  }

  if (
    datasource?.dataAccessConditions?.length ||
    datasource?.dataAccessConditionsDescription ||
    datasource?.releaseDescription ||
    datasource?.linkageOptions
  ) {
    tableOffContents.push({
      label: "Access Conditions",
      id: "access-conditions",
    });
  }

  if (datasource?.fundingStatement || datasource?.acknowledgements) {
    tableOffContents.push({
      label: "Funding & Citation requirements ",
      id: "funding-and-acknowledgement",
    });
  }

  if (datasource?.documentation) {
    tableOffContents.push({ label: "Attached files", id: "Files" });
  }

  return tableOffContents;
});

let accessConditionsItems = computed(() => {
  let items = [];
  if (datasource?.dataAccessConditions?.length) {
    items.push({
      label: "Conditions",
      content: datasource.dataAccessConditions.map((c) => c.name),
    });
  }
  if (datasource?.releaseDescription) {
    items.push({
      label: "Release",
      content: datasource.releaseDescription,
    });
  }
  if (datasource?.linkageOptions) {
    items.push({
      label: "Linkage options",
      content: datasource.linkageOptions,
    });
  }

  return items;
});

let fundingAndAcknowledgementItems = computed(() => {
  let items = [];
  if (datasource?.fundingStatement) {
    items.push({
      label: "Funding",
      content: datasource.fundingStatement,
    });
  }
  if (datasource?.acknowledgements) {
    items.push({
      label: "Citation requirements ",
      content: datasource.acknowledgements,
    });
  }

  return items;
});

useHead({ title: datasource?.acronym || datasource?.name });

const messageFilter = `{"filter": {"id":{"equals":"${route.params.cohort}"}}}`;

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
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        :title="datasource?.acronym || datasource?.name"
        :description="datasource?.acronym ? datasource?.name : ''"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="crumbs" :current="datasource.id" />
        </template>
        <!-- <template #title-suffix>
          <IconButton icon="star" label="Favorite" />
        </template> -->
      </PageHeader>
    </template>
    <template #side>
      <SideNavigation
        :title="datasource?.acronym || datasource?.name"
        :image="datasource?.logo?.url"
        :items="tocItems"
      />
    </template>
    <template #main>
      <ContentBlocks v-if="datasource">
        <ContentBlockIntro
          :image="datasource?.logo?.url"
          :link="datasource?.website"
          :contact="datasource?.contactEmail"
          :contact-name="datasource?.name"
          :contact-message-filter="messageFilter"
        />
        <ContentBlockDescription
          id="Description"
          title="Description"
          :description="datasource?.description"
        />

        <ContentBlockGeneralDesign
          id="GeneralDesign"
          title="General Design"
          :description="datasource?.designDescription"
          :cohort="datasource"
        />

        <ContentBlockContact
          v-if="datasource?.contacts"
          id="Contributors"
          title="Contact and Contributors"
          :contributors="datasource?.contacts"
        />

        <!-- <ContentBlockVariables
          id="Variables"
          title="Variables &amp; Topics"
          description="Explantation about variables and the functionality seen here."
        /> -->

        <ContentBlockData
          id="AvailableData"
          title="Available Data &amp; Samples"
          :collectionEvents="datasource?.collectionEvents"
        />

        <ContentBlockPartners
          v-if="datasource?.additionalOrganisations"
          id="Partners"
          title="Partners"
          description=""
          :partners="datasource?.additionalOrganisations"
        />

        <ContentBlockNetwork
          v-if="datasource?.networks"
          id="Networks"
          title="Networks"
          description="Networks Explanation about networks from this cohort and the functionality seen here."
          :networks="datasource?.networks"
        />

        <ContentBlock
          id="access-conditions"
          title="Access conditions"
          :description="datasource?.dataAccessConditionsDescription"
          v-if="
            datasource?.dataAccessConditions?.length ||
            datasource?.dataAccessConditionsDescription ||
            datasource?.releaseDescription
          "
        >
          <DefinitionList :items="accessConditionsItems" />
        </ContentBlock>

        <ContentBlock
          id="funding-and-acknowledgement"
          title="Funding &amp; Citation requirements "
          v-if="datasource?.fundingStatement || datasource?.acknowledgements"
        >
          <DefinitionList :items="fundingAndAcknowledgementItems" />
        </ContentBlock>

        <ContentBlockAttachedFiles
          v-if="datasource?.documentation?.length"
          id="Files"
          title="Attached Files"
          :documents="datasource.documentation"
        />
      </ContentBlocks> </template
    >f
  </LayoutsDetailPage>
</template>
