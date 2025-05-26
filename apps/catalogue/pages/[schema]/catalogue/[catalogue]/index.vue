<script setup lang="ts">
import { useRoute, useRuntimeConfig, useHead, useFetch } from "#app";
import { logError } from "#imports";
import { computed } from "vue";
import type { ISetting } from "../../../../../metadata-utils/src/types";

const route = useRoute();
const config = useRuntimeConfig();

const catalogueRouteParam = route.params.catalogue;

const scoped = route.params.catalogue !== "all";
const catalogue = scoped ? route.params.catalogue : undefined;

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting == "true" || config.public.cohortOnly;
});

//networksfilter retrieves the catalogues
//resources are within the current catalogue
const query = `query CataloguePage($networksFilter:ResourcesFilter,$variablesFilter:VariablesFilter,$collectionsFilter:ResourcesFilter,$networkFilter:ResourcesFilter){
        Resources(filter:$networksFilter) {
              id,
              acronym,
              name,
              description,
              logo {url}
       }
        Variables_agg(filter:$variablesFilter) {
          count
        }
        Collections_agg: Resources_agg(filter:$collectionsFilter) {
          count
          _sum {
            numberOfParticipants
            numberOfParticipantsWithSamples
          }
        }
        Networks_agg: Resources_agg(filter:$networkFilter) {
          count
        }
        Design_groupBy: Resources_groupBy(filter:$collectionsFilter) {
          design{name}
          count
        }
        Subpopulations_agg(filter:{resource: $collectionsFilter}) {
          count
        }
        _settings (keys: [
          "NOTICE_SETTING_KEY"
          "CATALOGUE_LANDING_TITLE"
          "CATALOGUE_LANDING_DESCRIPTION"
          "CATALOGUE_LANDING_COHORTS_CTA"
          "CATALOGUE_LANDING_COHORTS_TEXT"
          "CATALOGUE_LANDING_DATASOURCES_CTA"
          "CATALOGUE_LANDING_DATASOURCES_TEXT"
          "CATALOGUE_LANDING_NETWORKS_CTA"
          "CATALOGUE_LANDING_NETWORKS_TEXT"
          "CATALOGUE_LANDING_VARIABLES_CTA"
          "CATALOGUE_LANDING_VARIABLES_TEXT"
          "CATALOGUE_LANDING_PARTICIPANTS_LABEL"
          "CATALOGUE_LANDING_PARTICIPANTS_TEXT"
          "CATALOGUE_LANDING_SAMPLES_LABEL"
          "CATALOGUE_LANDING_SAMPLES_TEXT"
          "CATALOGUE_LANDING_DESIGN_LABEL"
          "CATALOGUE_LANDING_DESIGN_TEXT"
          "CATALOGUE_LANDING_SUBPOPULATIONS_LABEL"
          "CATALOGUE_LANDING_SUBPOPULATIONS_TEXT"
        ]){
          key
          value
        }
      }`;

const networksFilter = scoped
  ? { id: { equals: catalogueRouteParam } }
  : undefined;

const collectionsFilter = scoped
  ? {
      _and: [
        { type: { tags: { equals: "collection" } } },
        {
          _or: [
            { partOfResources: { id: { equals: catalogueRouteParam } } },
            {
              partOfResources: {
                partOfResources: { id: { equals: catalogueRouteParam } },
              },
            },
          ],
        },
      ],
    }
  : { type: { tags: { equals: "collection" } } };

const networkFilter = scoped
  ? {
      _and: [
        { type: { tags: { equals: "network" } } },
        {
          _or: [
            { partOfResources: { id: { equals: catalogueRouteParam } } },
            {
              partOfResources: {
                partOfResources: { id: { equals: catalogueRouteParam } },
              },
            },
          ],
        },
      ],
    }
  : { type: { tags: { equals: "network" } } };

const { data, error } = await useFetch(`/${route.params.schema}/graphql`, {
  method: "POST",
  key: `lading-page-${catalogueRouteParam}`,
  body: {
    query,
    variables: {
      networksFilter,
      collectionsFilter,
      networkFilter,
      variablesFilter: scoped
        ? {
            _or: [
              { resource: { id: { equals: catalogueRouteParam } } },
              //also include network of networks
              {
                resource: {
                  type: { name: { equals: "Network" } },
                  partOfResources: { id: { equals: catalogueRouteParam } },
                },
              },
              {
                reusedInResources: {
                  _or: [
                    { resource: { id: { equals: catalogueRouteParam } } },
                    {
                      resource: {
                        partOfResources: {
                          id: { equals: catalogueRouteParam },
                        },
                      },
                    },
                  ],
                },
              },
            ],
          }
        : //should only include harmonised variables
          { resource: { type: { name: { equals: "Network" } } } },
    },
  },
});

if (error.value) {
  const contextMsg = "Error on landing-page data fetch";
  if (error.value.data) {
    logError(error.value.data, contextMsg);
  }
  throw new Error(contextMsg);
}

function percentageLongitudinal(
  subpopulationsGroupBy: { count: number; design: { name: string } }[],
  total: number
) {
  const nLongitudinal = subpopulationsGroupBy.reduce(
    (accum, group) =>
      group?.design?.name === "Longitudinal" ? accum + group.count : accum,
    0
  );

  return Math.round((nLongitudinal / total) * 100);
}

function getSettingValue(settingKey: string, settings: ISetting[]) {
  return settings.find((setting: { key: string; value: string }) => {
    return setting.key === settingKey;
  })?.value;
}

const settings = computed(() => {
  return data.value.data._settings;
});

const network = computed(() => {
  return data.value.data?.Resources[0];
});

const title = computed(() => {
  if (catalogue) {
    return catalogue as string;
  } else if (getSettingValue("CATALOGUE_LANDING_TITLE", settings.value)) {
    return getSettingValue("CATALOGUE_LANDING_TITLE", settings.value) as string;
  } else {
    return "Browse all catalogue contents";
  }
});

const description = computed(() => {
  if (getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value)) {
    return getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value);
  } else {
    return "Select one of the content categories listed below.";
  }
});

useHead({
  title: scoped ? `${catalogue} Catalogue` : "Catalogue",
  meta: [
    {
      name: "description",
      content: scoped ? network.value?.description : description.value,
    },
  ],
});

const collectionCount = computed(() => data.value.data?.Collections_agg?.count);
const networkCount = computed(() => data.value.data?.Networks_agg?.count);

const aboutLink = `/${route.params.schema}/catalogue/${catalogueRouteParam}/networks/${catalogueRouteParam}`;
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader class="mx-auto lg:w-7/12 text-center" :title="title">
      <template v-if="scoped" v-slot:description
        >Welcome to the catalogue of
        <NuxtLink class="underline hover:bg-blue-50" :to="aboutLink">{{
          network.id
        }}</NuxtLink
        >{{ network.id && network.name ? ": " : "" }}{{ network.name }}. Select
        one of the content categories listed below.</template
      >
      <template v-else v-slot:description>
        <ContentReadMore :text="description" />
      </template>
    </PageHeader>
    <LandingPrimary>
      <LandingCardPrimary
        v-if="collectionCount"
        image="image-link"
        title="Collections"
        :description="
          getSettingValue('CATALOGUE_LANDING_COHORTS_TEXT', settings) ||
          'Data &amp; sample collections'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_COHORTS_CTA', settings) ||
          'Collections'
        "
        :count="collectionCount"
        :link="`/${route.params.schema}/catalogue/${catalogueRouteParam}/collections`"
      />
      <LandingCardPrimary
        v-if="networkCount && !cohortOnly"
        image="image-diagram"
        title="Networks"
        :description="
          getSettingValue('CATALOGUE_LANDING_NETWORKS_TEXT', settings) ||
          'Networks &amp; Consortia'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_NETWORKS_CTA', settings) ||
          'Networks'
        "
        :count="networkCount"
        :link="`/${route.params.schema}/catalogue/${catalogueRouteParam}/networks`"
      />
      <LandingCardPrimary
        v-if="data.data.Variables_agg?.count > 0 && !cohortOnly"
        image="image-diagram-2"
        title="Variables"
        :description="
          getSettingValue('CATALOGUE_LANDING_VARIABLES_TEXT', settings) ||
          'Harmonised variables'
        "
        :count="data.data.Variables_agg.count"
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_VARIABLES_CTA', settings)
        "
        :link="`/${route.params.schema}/catalogue/${catalogueRouteParam}/variables`"
      />
      <LandingCardPrimary
        v-if="!cohortOnly && network.id === 'FORCE-NEN collections'"
        image="image-data-warehouse"
        title="Aggregates"
        callToAction="Aggregates"
        :link="`/Aggregates/aggregates/#/`"
        :openLinkInNewTab="true"
      />
    </LandingPrimary>

    <LandingSecondary>
      <LandingCardSecondary
        icon="people"
        v-if="data.data.Collections_agg?._sum?.numberOfParticipants"
      >
        <b>
          {{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Collections_agg?._sum?.numberOfParticipants
            )
          }}
          {{
            getSettingValue("CATALOGUE_LANDING_PARTICIPANTS_LABEL", settings) ||
            "Participants"
          }}
        </b>
        <br />{{
          getSettingValue("CATALOGUE_LANDING_PARTICIPANTS_TEXT", settings) ||
          "The cumulative number of participants"
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="colorize"
        v-if="data.data.Collections_agg?._sum?.numberOfParticipantsWithSamples"
      >
        <b
          >{{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Collections_agg?._sum?.numberOfParticipantsWithSamples
            )
          }}
          {{
            getSettingValue("CATALOGUE_LANDING_SAMPLES_LABEL", settings) ||
            "Samples"
          }}</b
        >
        <br />{{
          getSettingValue("CATALOGUE_LANDING_SAMPLES_TEXT", settings) ||
          "The cumulative number of participants with samples"
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="schedule"
        v-if="data.data.Design_groupBy && data.data.Collections_agg"
      >
        <b
          >{{
            getSettingValue("CATALOGUE_LANDING_DESIGN_LABEL", settings) ||
            "Longitudinal"
          }}
          {{
            percentageLongitudinal(
              data.data.Design_groupBy,
              data.data.Collections_agg.count
            )
          }}%</b
        ><br />{{
          getSettingValue("CATALOGUE_LANDING_DESIGN_TEXT", settings) ||
          "Percentage of longitudinal datasets"
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="viewTable"
        v-if="data.data.Subpopulations_agg"
      >
        <b>
          {{ data.data.Subpopulations_agg.count }}
          {{
            getSettingValue(
              "CATALOGUE_LANDING_SUBPOPULATIONS_LABEL",
              settings
            ) || "Subpopulations"
          }}
        </b>
        <br />
        {{
          getSettingValue("CATALOGUE_LANDING_SUBPOPULATIONS_TEXT", settings) ||
          "The total number of subpopulations included"
        }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
