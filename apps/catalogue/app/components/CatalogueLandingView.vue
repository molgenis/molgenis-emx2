<script setup lang="ts">
import { useRuntimeConfig, useHead, useFetch, createError } from "#app";
import { logError, useCatalogueContext } from "#imports";
import { computed } from "vue";
import type { ISetting } from "../../../../metadata-utils/src/types";
import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import LandingPrimary from "../components/landing/Primary.vue";
import LandingSecondary from "../components/landing/Secondary.vue";
import LandingCardPrimary from "../components/landing/CardPrimary.vue";
import LandingCardSecondary from "../components/landing/CardSecondary.vue";
import ContentReadMore from "../../../../tailwind-components/app/components/ContentReadMore.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import ShowMore from "../../../../tailwind-components/app/components/ShowMore.vue";
import ContentReadMore from "../../../../tailwind-components/app/components/ContentReadMore.vue";

interface Props {
  resourceId: string;
}

const props = defineProps<Props>();

const config = useRuntimeConfig();
const schema = config.public.schema as string;
const { resourceUrl } = useCatalogueContext();

const cohortOnly = computed(() => {
  return config.public.cohortOnly;
});

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
          "CATALOGUE_ALL_ADDITIONAL_HTML"
        ]){
          key
          value
        }
      }`;

const networksFilter = { id: { equals: props.resourceId } };

const collectionsFilter = {
  _and: [
    { type: { tags: { equals: "collection" } } },
    {
      _or: [
        { partOfNetworks: { id: { equals: props.resourceId } } },
        {
          partOfNetworks: {
            parentNetworks: { id: { equals: props.resourceId } },
          },
        },
      ],
    },
  ],
};

const networkFilter = {
  _and: [
    { type: { tags: { equals: "network" } } },
    {
      _or: [
        { parentNetworks: { id: { equals: props.resourceId } } },
        {
          parentNetworks: {
            parentNetworks: { id: { equals: props.resourceId } },
          },
        },
      ],
    },
  ],
};

const { data, error } = await useFetch(`/${schema}/graphql`, {
  method: "POST",
  key: `landing-page-${props.resourceId}`,
  body: {
    query,
    variables: {
      networksFilter,
      collectionsFilter,
      networkFilter,
      variablesFilter: {
        _or: [
          { resource: { id: { equals: props.resourceId } } },
          {
            resource: {
              type: { name: { equals: "Network" } },
              parentNetworks: { id: { equals: props.resourceId } },
            },
          },
          {
            reusedInResources: {
              _or: [
                { resource: { id: { equals: props.resourceId } } },
                {
                  resource: {
                    parentNetworks: {
                      id: { equals: props.resourceId },
                    },
                  },
                },
              ],
            },
          },
        ],
      },
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
  const resources = data.value.data?.Resources;
  if (scoped && (!resources || resources.length === 0)) {
    throw createError({
      statusCode: 404,
      statusMessage: 'Catalogue "' + props.resourceId + '" Not Found.',
    });
  }
  return resources?.[0];
});

const title = computed(() => {
  if (getSettingValue("CATALOGUE_LANDING_TITLE", settings.value)) {
    return getSettingValue("CATALOGUE_LANDING_TITLE", settings.value) as string;
  } else {
    return props.resourceId;
  }
});

const description = computed(() => {
  if (getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value)) {
    return getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value);
  } else if (scoped && network.value?.description) {
    return network.value?.description;
  } else {
    return "Select one of the content categories listed below.";
  }
});

useHead({
  title: `${props.resourceId} Catalogue`,
  meta: [
    {
      name: "description",
      content: network.value?.description,
    },
  ],
});

const collectionCount = computed(() => data.value.data?.Collections_agg?.count);
const networkCount = computed(() => data.value.data?.Networks_agg?.count);

const aboutLink = resourceUrl(`${props.resourceId}/about`);
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader class="mx-auto lg:w-7/12 text-center" :title="title">
      <template v-slot:description
        >Welcome to the catalogue of
        <NuxtLink class="underline hover:bg-link-hover" :to="aboutLink">{{
          network.id
        }}</NuxtLink
        >{{ network.id && network.name ? ": " : "" }}{{ network.name }}. Select
        one of the content categories listed below.</template
      >
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
        :link="resourceUrl(`${resourceId}/collections`)"
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
        :link="resourceUrl(`${resourceId}/variables`)"
      />
      <LandingCardPrimary
        v-if="!cohortOnly && scoped && network.id === 'FORCE-NEN collections'"
        image="image-data-warehouse"
        title="Aggregates"
        callToAction="Aggregates"
        link="/Aggregates/aggregates/#/"
        :isExternalLink="true"
        :openLinkInNewTab="true"
      />
    </LandingPrimary>

    <div
      v-if="getSettingValue('CATALOGUE_ALL_ADDITIONAL_HTML', settings)"
      v-html="getSettingValue('CATALOGUE_ALL_ADDITIONAL_HTML', settings)"
    />

    <LandingSecondary>
      <LandingCardSecondary
        icon="people"
        v-if="data.data.Collections_agg?._sum?.numberOfParticipants"
      >
        <b>
          {{
            new Intl.NumberFormat("en-GB").format(
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
            new Intl.NumberFormat("en-GB").format(
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
