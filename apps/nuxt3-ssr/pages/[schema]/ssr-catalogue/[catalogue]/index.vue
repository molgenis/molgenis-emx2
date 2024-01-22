<script setup lang="ts">
import type { ISetting } from "meta-data-utils";
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

const query = `query MyQuery($networksFilter:NetworksFilter,$variablesFilter:VariablesFilter,$cohortsFilter:CohortsFilter,$subcohortsFilter:SubcohortsFilter,$dataSourcesFilter:DataSourcesFilter){
        Networks(filter:$networksFilter) {
              id,
              acronym,
              name,
              description,
              logo {url}
              dataSources_agg{count}
              networks_agg{count}
       }
        Variables_agg(filter:$variablesFilter) {
          count
        }
        Cohorts_agg(filter:$cohortsFilter) {
          count
          sum {
            numberOfParticipants
            numberOfParticipantsWithSamples
          }
        }
        DataSources_agg(filter:$dataSourcesFilter) {
          count
        }
        Datasets_agg {
          count
        }
        Subcohorts_agg(filter:$subcohortsFilter){
          count
        }
        Networks_agg {
          count
        }
        Organisations_agg {
          count
        }
        Models_agg {
          count
        }
        Cohorts_groupBy(filter:$cohortsFilter) {
          count
          design {
            name
          }
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

const modelFilter = scoped ? { id: { equals: catalogueRouteParam } } : {};
const networksFilter = scoped
  ? { id: { equals: catalogueRouteParam } }
  : undefined;

const cohortsFilter = scoped
  ? { networks: { id: { equals: catalogueRouteParam } } }
  : undefined;
const subcohortsFilter = scoped
  ? {
      resource: {
        id: { equals: "cannot make a filter, todo fix data model" },
      },
    }
  : undefined;

const dataSourcesFilter = scoped
  ? { networks: { id: { equals: catalogueRouteParam } } }
  : undefined;

const { data, error } = await useAsyncData<any, IMgError>(
  `lading-page-${catalogueRouteParam}`,
  async () => {
    const models = await $fetch(`/${route.params.schema}/catalogue/graphql`, {
      baseURL: config.public.apiBase,
      method: "POST",
      body: {
        query: `
            query Networks($filter:NetworksFilter) {
              Networks(filter:$filter){models{id}}
            }`,
        variables: { filter: modelFilter },
      },
    });

    const variablesFilter = scoped
      ? {
          resource: {
            id: {
              equals: models.data.Networks[0].models
                ? models.data.Networks[0].models.map(
                    (m: { id: string }) => m.id
                  )
                : "no models match so no results expected",
            },
          },
        }
      : undefined;

    return $fetch(`/${route.params.schema}/catalogue/graphql`, {
      baseURL: config.public.apiBase,
      method: "POST",
      body: {
        query,
        variables: {
          networksFilter,
          variablesFilter,
          cohortsFilter,
          subcohortsFilter,
          dataSourcesFilter,
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
  cohortsGroupBy: { count: number; design: { name: string } }[],
  total: number
) {
  const nLongitudinal = cohortsGroupBy.reduce(
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
  return data.value.data?.Networks[0];
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

let description = computed(() => {
  if (getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value)) {
    return getSettingValue("CATALOGUE_LANDING_DESCRIPTION", settings.value);
  } else {
    return "Select one of the content categories listed below.";
  }
});

const numberOfNetworks = computed(() => {
  return scoped
    ? data.value.data.Networks[0]?.networks_agg.count
    : data.value.data.Networks_agg?.count;
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
      <template v-else v-slot:description
        ><ReadMore>{{ description }}</ReadMore></template
      >
    </PageHeader>

    <LandingPrimary>
      <LandingCardPrimary
        v-if="data.data.Cohorts_agg.count > 0"
        image="image-link"
        title="Cohorts"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_COHORTS_TEXT',
            data.data._settings
          ) || 'Cohorts &amp; Biobanks'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_COHORTS_CTA', data.data._settings)
        "
        :count="data.data.Cohorts_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/cohorts`"
      />
      <LandingCardPrimary
        v-if="data.data.DataSources_agg.count > 0 && !cohortOnly"
        image="image-data-warehouse"
        title="Data sources"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_DATASOURCES_TEXT',
            data.data._settings
          ) || 'Databanks &amp; Registries'
        "
        :callToAction="
          getSettingValue(
            'CATALOGUE_LANDING_DATASOURCES_CTA',
            data.data._settings
          )
        "
        :count="data.data.DataSources_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/datasources`"
      />
      <LandingCardPrimary
        v-if="data.data.Variables_agg.count > 0 && !cohortOnly"
        image="image-diagram-2"
        title="Variables"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_VARIABLES_TEXT',
            data.data._settings
          ) || 'Harmonized variables'
        "
        :count="data.data.Variables_agg.count"
        :callToAction="
          getSettingValue(
            'CATALOGUE_LANDING_VARIABLES_CTA',
            data.data._settings
          )
        "
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`"
      />

      <LandingCardPrimary
        v-if="numberOfNetworks > 0 && !cohortOnly"
        image="image-diagram"
        title="Networks"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_NETWORKS_TEXT',
            data.data._settings
          ) || 'Networks &amp; consortia'
        "
        :count="numberOfNetworks"
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_NETWORKS_CTA', data.data._settings)
        "
        :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks`"
      />
    </LandingPrimary>

    <LandingSecondary>
      <LandingCardSecondary
        icon="people"
        v-if="data.data.Cohorts_agg?.sum?.numberOfParticipants"
      >
        <b>
          {{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Cohorts_agg?.sum?.numberOfParticipants
            )
          }}
          {{
            getSettingValue(
              "CATALOGUE_LANDING_PARTICIPANTS_LABEL",
              data.data._settings
            ) || "Participants"
          }}
        </b>
        <br />{{
          getSettingValue(
            "CATALOGUE_LANDING_PARTICIPANTS_TEXT",
            data.data._settings
          ) ||
          "The cumulative number of participants of all (sub)cohorts combined."
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="colorize"
        v-if="data.data.Cohorts_agg?.sum?.numberOfParticipantsWithSamples"
      >
        <b
          >{{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Cohorts_agg?.sum?.numberOfParticipantsWithSamples
            )
          }}
          {{
            getSettingValue(
              "CATALOGUE_LANDING_SAMPLES_LABEL",
              data.data._settings
            ) || "Samples"
          }}</b
        >
        <br />{{
          getSettingValue(
            "CATALOGUE_LANDING_SAMPLES_TEXT",
            data.data._settings
          ) ||
          "The cumulative number of participants with samples collected of all (sub)cohorts combined"
        }}
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="schedule"
        v-if="data.data.Cohorts_groupBy && data.data.Cohorts_agg.count"
      >
        <b
          >{{
            getSettingValue(
              "CATALOGUE_LANDING_DESIGN_LABEL",
              data.data._settings
            ) || "Longitudinal"
          }}
          {{
            percentageLongitudinal(
              data.data.Cohorts_groupBy,
              data.data.Cohorts_agg.count
            )
          }}%</b
        ><br />{{
          getSettingValue(
            "CATALOGUE_LANDING_DESIGN_TEXT",
            data.data._settings
          ) || "Percentage of longitudinal datasets. The remaining datasets are"
        }}
        cross-sectional.
      </LandingCardSecondary>

      <LandingCardSecondary
        icon="viewTable"
        v-if="data.data.Subcohorts_agg.count"
      >
        <b>
          {{ data.data.Subcohorts_agg.count }}
          {{
            getSettingValue(
              "CATALOGUE_LANDING_SUBCOHORTS_LABEL",
              data.data._settings
            ) || "Subcohorts"
          }}
        </b>
        <br />
        {{
          getSettingValue(
            "CATALOGUE_LANDING_SUBCOHORTS_TEXT",
            data.data._settings
          ) || "The total number of subcohorts included"
        }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
