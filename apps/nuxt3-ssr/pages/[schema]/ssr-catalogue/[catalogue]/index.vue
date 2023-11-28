<script setup lang="ts">
import type { ISetting } from "meta-data-utils";

const route = useRoute();
const config = useRuntimeConfig();

const catalogueRouteParam = route.params.catalogue;

const cohortOnly = computed(() => {
  const routeSetting = route.query["cohort-only"] as string;
  return routeSetting == "true" || config.public.cohortOnly;
});

const modelFilter =
  catalogueRouteParam === "all" ? {} : { id: { equals: catalogueRouteParam } };
const modelQuery = `
  query Networks($filter:NetworksFilter) {
    Networks(filter:$filter){models{id}}
  }`;

const models = await fetchGql(modelQuery, { filter: modelFilter });

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

const data = await fetchGql(query, {
  networksFilter:
    "all" === catalogueRouteParam
      ? {}
      : { id: { equals: catalogueRouteParam } },
  variablesFilter:
    "all" === catalogueRouteParam
      ? {}
      : {
          resource: {
            id: {
              equals: models.data.Networks[0].models
                ? models.data.Networks[0].models.map((m) => m.id)
                : "no models match so no results expected",
            },
          },
        },
  cohortsFilter:
    "all" === catalogueRouteParam
      ? {}
      : { networks: { id: { equals: catalogueRouteParam } } },
  subcohortsFilter:
    "all" === catalogueRouteParam
      ? {}
      : {},
  dataSourcesFilter:
    "all" === catalogueRouteParam
      ? {}
      : { networks: { id: { equals: catalogueRouteParam } } },
});

const catalogue = "all" === catalogueRouteParam ? {} : data.data?.Networks[0];
const networksCount : number = "all" === catalogueRouteParam ? data.data?.Networks_agg.count : data.data?.Networks[0].networks_agg.count;

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

let title = computed(() => {
  if (catalogue?.name) {
    return `${
      catalogue.acronym && catalogue.acronym !== catalogue.name
        ? catalogue.acronym + ":"
        : ""
    } ${catalogue.name}`;
  } else if (getSettingValue("CATALOGUE_LANDING_TITLE", data.data._settings)) {
    return getSettingValue("CATALOGUE_LANDING_TITLE", data.data._settings);
  } else {
    return "Browse all catalogue contents";
  }
});

let description = computed(() => {
  if (catalogue?.description) {
    return catalogue.description;
  } else if (
    getSettingValue("CATALOGUE_LANDING_DESCRIPTION", data.data._settings)
  ) {
    return getSettingValue(
      "CATALOGUE_LANDING_DESCRIPTION",
      data.data._settings
    );
  } else {
    return "Select one of the content categories listed below.";
  }
});
</script>

<template>
  <Main>
    <LayoutsLandingPage class="w-10/12 pt-8">
      <PageHeader
        class="mx-auto lg:w-7/12 text-center"
        :title="title"
        :description="description"
      ></PageHeader>
      <LandingPrimary>
        <LandingCardPrimary
          v-if="data.data.Cohorts_agg.count > 0"
          image="image-link"
          title="Cohorts"
          :description="
            getSettingValue(
              'CATALOGUE_LANDING_COHORTS_TEXT',
              data.data._settings
            ) ||
            ' A complete overview of ' +
              catalogueRouteParam +
              ' cohorts and biobanks.'
          "
          :callToAction="
            getSettingValue(
              'CATALOGUE_LANDING_COHORTS_CTA',
              data.data._settings
            )
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
            ) || catalogueRouteParam + ' databanks and registries'
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
            v-if="networksCount > 0 && !cohortOnly"
            image="image-diagram-2"
            title="Networks"
            :description="
            getSettingValue(
              'CATALOGUE_LANDING_NETWORKS_TEXT',
              data.data._settings
            ) || catalogueRouteParam + ' networks.'
          "
            :count="networksCount"
            :callToAction="
            getSettingValue(
              'CATALOGUE_LANDING_NETWORKS_CTA',
              data.data._settings
            )
          "
            :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks`"
        />
        <LandingCardPrimary
          v-if="data.data.Variables_agg.count > 0 && !cohortOnly"
          image="image-diagram-2"
          title="Variables"
          :description="
            getSettingValue(
              'CATALOGUE_LANDING_VARIABLES_TEXT',
              data.data._settings
            ) || catalogueRouteParam + ' harmonized variables.'
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
            ) ||
            "Percentage of longitudinal datasets. The remaining datasets are"
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
        <!-- todo
        <LandingCardSecondary
          icon="demography"
          title="Cohort studies"
          :count="data.data.Cohorts_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/cohorts`"
        />
        <LandingCardSecondary
          icon="database"
          title="Data sources"
          :count="data.data.DataSources_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/datasources`"
        />

        <LandingCardSecondary
          icon="hub"
          title="Networks"
          :count="data.data.Networks_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/networks`"
        />
        <LandingCardSecondary
          icon="institution"
          title="Organisations"
          :count="data.data.Organisations_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/organisations`"
        />
        <LandingCardSecondary
          icon="dataset"
          title="Datasets"
          :count="data.data.Cohorts_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/datasets`"
        />
        <LandingCardSecondary
          icon="list"
          title="Collected variables"
          :count="data.data.Networks_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`"
        />-->
        <!-- todo must split in collected and harmonized -->
        <!--
        <LandingCardSecondary
          icon="harmonized-variables"
          title="Harmonized variables"
          :count="data.data.Variables_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/variables`"
        />
        <LandingCardSecondary
          icon="dataset-linked"
          title="Standards"
          :count="data.data.Models_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${catalogueRouteParam}/models`"
        />-->
      </LandingSecondary>
    </LayoutsLandingPage>
  </Main>
</template>
