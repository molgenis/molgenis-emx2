<script setup lang="ts">
import type { IMgError } from "~~/interfaces/types";

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
//resources are within the current catalogue
const query = `query CataloguePage($networksFilter:ResourcesFilter,$variablesFilter:VariablesFilter,$resourceFilter:ResourcesFilter){
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
        Resources_agg(filter:$resourceFilter) {
          count
          _sum {
            numberOfParticipants
            numberOfParticipantsWithSamples
          }
        }
        Resources_groupBy(filter:$resourceFilter) {
          type{name,definition}
          count
        }
        Design_groupBy:Resources_groupBy(filter:$resourceFilter) {
          design{name}
          count
        }
        Subpopulations_agg(filter:{resource: $resourceFilter}) {
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

const resourceFilter = scoped
  ? {
      _or: [
        { partOfResources: { id: { equals: catalogueRouteParam } } },
        {
          partOfResources: {
            partOfResources: { id: { equals: catalogueRouteParam } },
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
            { resource: { id: { equals: catalogueRouteParam } } },
            //also include network of networks
            {
              resource: {
                type: { name: { equals: "Network" } },
                partOfResources: { id: { equals: catalogueRouteParam } },
              },
            },
          ],
        }
      : //should only include harmonised variables
        { resource: { type: { name: { equals: "Network" } } } };

    return $fetch(`/${route.params.schema}/graphql`, {
      method: "POST",
      body: {
        query,
        variables: {
          networksFilter,
          variablesFilter,
          resourceFilter,
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
        v-for="resource in data.data.Resources_groupBy"
        :image="
          getResourceMetadataForType(resource.type.name).image || 'image-link'
        "
        :title="
          getResourceMetadataForType(resource.type.name)?.plural ||
          resource.type.name
        "
        :description="
          getSettingValue('CATALOGUE_LANDING_COHORTS_TEXT', settings) ||
          getResourceMetadataForType(resource.type.name).description ||
          'Cohorts &amp; Biobanks'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_COHORTS_CTA', settings)
        "
        :count="resource.count"
        :link="
          `/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/` +
          (getResourceMetadataForType(resource.type.name).path || 'resources')
        "
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
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`"
      />

      <LandingCardPrimary
        v-if="network.id === 'FORCE-NEN collections'"
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
        v-if="data.data.Resources_agg?._sum?.numberOfParticipants"
      >
        <b>
          {{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Resources_agg?._sum?.numberOfParticipants
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
        v-if="data.data.Resources_agg?._sum?.numberOfParticipantsWithSamples"
      >
        <b
          >{{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Resources_agg?._sum?.numberOfParticipantsWithSamples
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
        v-if="data.data.Design_groupBy && data.data.Resources_agg"
      >
        <b
          >{{
            getSettingValue("CATALOGUE_LANDING_DESIGN_LABEL", settings) ||
            "Longitudinal"
          }}
          {{
            percentageLongitudinal(
              data.data.Design_groupBy,
              data.data.Resources_agg.count
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
        v-if="data.data.Subpopulations?.count"
      >
        <b>
          {{ data.data.Subpopulations_agg.count }}
          {{
            getSettingValue("CATALOGUE_LANDING_COHORTS_LABEL", settings) ||
            "Cohorts"
          }}
        </b>
        <br />
        {{
          getSettingValue("CATALOGUE_LANDING_COHORTS_TEXT", settings) ||
          "The total number of cohorts included"
        }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
