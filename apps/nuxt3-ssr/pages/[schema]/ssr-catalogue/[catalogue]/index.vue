<script setup lang="ts">
import type { IMgError } from "~~/interfaces/types";
import { getCollectionMetadataForType } from "~/constants";

const route = useRoute();
const config = useRuntimeConfig();

const catalogueRouteParam = route.params.catalogue;

const scoped = route.params.catalogue !== "all";
const catalogue = scoped ? route.params.catalogue : undefined;

useHead({ title: scoped ? `${catalogue} Catalogue` : "Catalogue" });

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting == "true" || config.public.cohortOnly;
});

//networksfilter retrieves the catalogues
//collections are within the current catalogue
const query = `query CataloguePage($networksFilter:CollectionsFilter,$variablesFilter:VariablesFilter,$cohortsFilter:CollectionsFilter){
        Collections(filter:$networksFilter) {
              id,
              acronym,
              name,
              description,
              logo {url}
       }
        Variables_agg(filter:$variablesFilter) {
          count
        }
        Collections_agg(filter:$cohortsFilter) {
          count
          _sum {
            numberOfParticipants
            numberOfParticipantsWithSamples
          }
        }
        Collections_groupBy(filter:$cohortsFilter) {
          type{name,definition}
          count
        }
        CollectionSubcohorts_agg(filter:{collection: $cohortsFilter}) {
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
          "CATALOGUE_LANDING_SUBCOHORTS_LABEL"
          "CATALOGUE_LANDING_SUBCOHORTS_TEXT"
        ]){
          key
          value
        }
      }`;

const networksFilter = scoped
  ? { id: { equals: catalogueRouteParam } }
  : undefined;

const cohortsFilter = scoped
  ? {
      _or: [
        { partOfCollections: { id: { equals: catalogueRouteParam } } },
        {
          partOfCollections: {
            partOfCollections: { id: { equals: catalogueRouteParam } },
          },
        },
      ],
    }
  : undefined;

const { data, error } = await useAsyncData<any, IMgError>(
  `lading-page-${catalogueRouteParam}`,
  async () => {
    const variablesFilter = scoped
      ? {
          _or: [
            { collection: { id: { equals: catalogueRouteParam } } },
            //also include network of networks
            {
              collection: {
                type: { name: { equals: "Network" } },
                partOfCollections: { id: { equals: catalogueRouteParam } },
              },
            },
          ],
        }
      : //should only include harmonised variables
        { collection: { type: { name: { equals: "Network" } } } };

    return $fetch(`/${route.params.schema}/graphql`, {
      method: "POST",
      body: {
        query,
        variables: {
          networksFilter,
          variablesFilter,
          cohortsFilter,
        },
      },
    });
  }
);

if (error.value) {
  const contextMsg = "Error on landing-page data fetch";
  logError(error.value, contextMsg);
  throw new Error(contextMsg);
}

function percentageLongitudinal(
  cohortsGroupBy: { count: number; designType: { name: string } }[],
  total: number
) {
  const nLongitudinal = cohortsGroupBy.reduce(
    (accum, group) =>
      group?.designType?.name === "Longitudinal" ? accum + group.count : accum,
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
  return data.value.data?.Collections[0];
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

const aboutLink = `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks/${catalogueRouteParam}`;
</script>

<template>
  <LayoutsLandingPage class="w-10/12 pt-8">
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
        v-for="collection in data.data.Collections_groupBy"
        :image="
          getCollectionMetadataForType(collection.type.name).image ||
          'image-link'
        "
        :title="
          getCollectionMetadataForType(collection.type.name)?.plural ||
          collection.type.name
        "
        :description="
          getSettingValue('CATALOGUE_LANDING_COHORTS_TEXT', settings) ||
          getCollectionMetadataForType(collection.type.name).description ||
          'Cohorts &amp; Biobanks'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_COHORTS_CTA', settings)
        "
        :count="collection.count"
        :link="
          `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/` +
          (getCollectionMetadataForType(collection.type.name).path ||
            'collections')
        "
      />
      <LandingCardPrimary
        v-if="data.data.Variables_agg.count > 0 && !cohortOnly"
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
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`"
      />
    </LandingPrimary>

    <LandingCardPrimary
      v-if="network.id === 'FORCE-NEN collections'"
      image="image-data-warehouse"
      title="Aggregates"
      callToAction="Aggregates"
      :link="`/Aggregates/aggregates/#/`"
    />

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
          "The cumulative number of participants of all (sub)cohorts combined."
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
          "The cumulative number of participants with samples collected of all (sub)cohorts combined"
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="schedule"
        v-if="data.data.Cohorts_groupBy && data.data.Cohorts_agg.count"
      >
        <b
          >{{
            getSettingValue("CATALOGUE_LANDING_DESIGN_LABEL", settings) ||
            "Longitudinal"
          }}
          {{
            percentageLongitudinal(
              data.data.Collections_groupBy,
              data.data.Collections_agg.count
            )
          }}%</b
        ><br />{{
          getSettingValue("CATALOGUE_LANDING_DESIGN_TEXT", settings) ||
          "Percentage of longitudinal datasets. The remaining datasets are"
        }}
        cross-sectional.
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="viewTable"
        v-if="data.data.CollectionSubcohorts_agg.count"
      >
        <b>
          {{ data.data.CollectionSubcohorts_agg.count }}
          {{
            getSettingValue("CATALOGUE_LANDING_SUBCOHORTS_LABEL", settings) ||
            "Subcohorts"
          }}
        </b>
        <br />
        {{
          getSettingValue("CATALOGUE_LANDING_SUBCOHORTS_TEXT", settings) ||
          "The total number of subcohorts included"
        }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
