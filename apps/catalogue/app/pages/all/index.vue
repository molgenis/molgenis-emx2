<script setup lang="ts">
import { useRuntimeConfig, useHead, useFetch } from "#app";
import { logError } from "#imports";
import { computed } from "vue";
import type { ISetting } from "../../../../metadata-utils/src/types";
import LayoutsLandingPage from "../../components/layouts/LandingPage.vue";
import LandingPrimary from "../../components/landing/Primary.vue";
import LandingSecondary from "../../components/landing/Secondary.vue";
import LandingCardPrimary from "../../components/landing/CardPrimary.vue";
import LandingCardSecondary from "../../components/landing/CardSecondary.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import ContentReadMore from "../../../../tailwind-components/app/components/ContentReadMore.vue";

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const cohortOnly = computed(() => {
  return config.public.cohortOnly;
});

const query = `query CataloguePage($variablesFilter:VariablesFilter,$collectionsFilter:ResourcesFilter,$networkFilter:ResourcesFilter){
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

const collectionsFilter = { type: { tags: { equals: "collection" } } };

const networkFilter = { type: { tags: { equals: "network" } } };

const { data, error } = await useFetch(`/${schema}/graphql`, {
  method: "POST",
  key: `landing-page-all`,
  body: {
    query,
    variables: {
      collectionsFilter,
      networkFilter,
      variablesFilter: { resource: { type: { name: { equals: "Network" } } } },
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

const title = computed(() => {
  if (getSettingValue("CATALOGUE_LANDING_TITLE", settings.value)) {
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
  title: "Catalogue",
  meta: [
    {
      name: "description",
      content: description.value,
    },
  ],
});

const collectionCount = computed(() => data.value.data?.Collections_agg?.count);
const networkCount = computed(() => data.value.data?.Networks_agg?.count);
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader class="mx-auto lg:w-7/12 text-center" :title="title">
      <template v-slot:description>
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
        link="/all/collections?catalogue=all"
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
        link="/all/variables?catalogue=all"
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
