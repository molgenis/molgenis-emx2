<script setup lang="ts">
import type {ISetting} from "meta-data-utils";

const route = useRoute();
const config = useRuntimeConfig();

const cat = route.params.catalogue;

let graphqlURL = computed(() => `/${route.params.schema}/catalogue/graphql`);

const modelFilter = cat === 'all' ? {} : {id:{equals: cat}}
const modelQuery = `
  query Networks($filter:NetworksFilter) {
    Networks(filter:$filter){models{id}}
  }`;

const models = await fetchGql(
    modelQuery,
    {filter: modelFilter}
);

const query = `query MyQuery($networkFilter:NetworksFilter,$variablesFilter:VariablesFilter,$cohortsFilter:CohortsFilter,$subcohortsFilter:SubcohortsFilter,$dataSourcesFilter:DataSourcesFilter){
        Networks(filter:$networkFilter) {
              id,
              acronym,
              name,
              description,
              logo {url}
              dataSources_agg{count}
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
console.log(query);

const networkFilter = "all" === cat ? "" : `(filter:{id:{equals:"${cat}"}})`;
const resourceFilter =
    "all" === cat ? "" : `(filter:{networks:{id:{equals:"${cat}"}}})`;
const variablesFilter =
    "all" === cat ? "" : `(filter:{resource:{id:{equals:"${cat}"}}})`; //todo: better mapping from variable to network

const data = await fetchGql(
  query,
    {
      networksFilter: 'all' === cat ? {} : {id:{equals:cat}},
      variablesFilter: 'all' === cat ? {} : {resource: {id: {equals: models.data.Networks[0].models.map(m => m.id)}}},
      cohortsFilter: {},
      subcohortsFilter: {},
      dataSourcesFilter: {}
    }
);
console.log(data);
const catalogue = "all" === cat ? {} : data.data?.Networks[0];

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
    return `${catalogue.acronym && catalogue.acronym !== catalogue.name ? catalogue.acronym + ":" : ""} ${
      catalogue.name
    }`;
  } else if (
    getSettingValue("CATALOGUE_LANDING_TITLE", data.value.data._settings)
  ) {
    return getSettingValue(
      "CATALOGUE_LANDING_TITLE",
      data.value.data._settings
    );
  } else {
    return "European Networks Health Data & Cohort Catalogue.";
  }
});

let description = computed(() => {
  if (catalogue?.description) {
    return catalogue.description;
  } else if (
    getSettingValue("CATALOGUE_LANDING_DESCRIPTION", data.value.data._settings)
  ) {
    getSettingValue("CATALOGUE_LANDING_DESCRIPTION", data.value.data._settings);
  } else {
    return "Browse metadata for data resources in this catalogue.";
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
          image="demography"
          title="Cohorts"
          :description="
            'Browse ' +
            cat +
            ' catalogued population and disease specific cohort studies'
          "
          :count="data.data.Cohorts_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/cohorts`"
        />
        <LandingCardPrimary
          image="clinical"
          title="Data sources"
          :description="
            'Browse ' +
            cat +
            ' catalogued health and population databanks and registries'
          "
          :count="data.data.DataSources_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/datasources`"
        />
        <LandingCardPrimary
          image="checklist"
          title="Variables"
          :description="'A listing of ' + cat + ' harmonized variables.'"
          :count="data.data.Variables_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/variables`"
        />
      </LandingPrimary>
      <LandingSecondary>
        <LandingCardSecondary icon="people" v-if="data.data.Cohorts_agg?.sum?.numberOfParticipants">
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

        <LandingCardSecondary icon="colorize" v-if="data.data.Cohorts_agg?.sum?.numberOfParticipantsWithSamples">
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

        <LandingCardSecondary icon="schedule" v-if="data.data.Cohorts_groupBy && data.data.Cohorts_agg.count">
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

        <LandingCardSecondary icon="viewTable" v-if="data.data.Subcohorts_agg.count">
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
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/cohorts`"
        />
        <LandingCardSecondary
          icon="database"
          title="Data sources"
          :count="data.data.DataSources_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/datasources`"
        />

        <LandingCardSecondary
          icon="hub"
          title="Networks"
          :count="data.data.Networks_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/networks`"
        />
        <LandingCardSecondary
          icon="institution"
          title="Organisations"
          :count="data.data.Organisations_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/organisations`"
        />
        <LandingCardSecondary
          icon="dataset"
          title="Datasets"
          :count="data.data.Cohorts_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/datasets`"
        />
        <LandingCardSecondary
          icon="list"
          title="Collected variables"
          :count="data.data.Networks_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/variables`"
        />-->
        <!-- todo must split in collected and harmonized -->
        <!--
        <LandingCardSecondary
          icon="harmonized-variables"
          title="Harmonized variables"
          :count="data.data.Variables_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/variables`"
        />
        <LandingCardSecondary
          icon="dataset-linked"
          title="Standards"
          :count="data.data.Models_agg.count"
          :link="`/${route.params.schema}/ssr-catalogue/${cat}/models`"
        />-->
      </LandingSecondary>
    </LayoutsLandingPage>
  </Main>
</template>
