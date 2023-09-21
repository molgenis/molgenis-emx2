<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: {
      query: `{
        Variables_agg {
          count
        }
        Cohorts_agg { 
          count
          sum {
            numberOfParticipants
            numberOfParticipantsWithSamples 
          }
        }
        Subcohorts_agg {
          count
        }
        Networks_agg { 
          count
        }
        Cohorts_groupBy {
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
      }`,
    },
  }
);

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
</script>
<template>
  <LayoutsLandingPage class="w-10/12 pt-8">
    <PageHeader
      class="mx-auto lg:w-7/12 text-center"
      :title="
        getSettingValue('CATALOGUE_LANDING_TITLE', data.data._settings) ||
        'European Networks Health Data & Cohort Catalogue.'
      "
      :description="
        getSettingValue('CATALOGUE_LANDING_DESCRIPTION', data.data._settings) ||
        'Browse and manage metadata for data resources, such as cohorts, registries, biobanks, and multi-center collaborations thereof such as networks, common data models and studies.'
      "
    ></PageHeader>

    <div
      class="bg-white relative justify-around flex flex-col md:flex-row px-5 py-12 antialiased lg:pb-10 lg:px-0 rounded-t-3px rounded-b-landing shadow-primary"
    >
      <LandingCardPrimary
        image="image-link"
        title="Cohorts"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_COHORTS_TEXT',
            data.data._settings
          ) || 'A complete overview of all cohorts and biobanks.'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_COHORTS_CTA', data.data._settings)
        "
        :count="data.data.Cohorts_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/cohorts/`"
      />
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="image-diagram"
        title="Networks"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_NETWORKS_TEXT',
            data.data._settings
          ) ||
          'Collaborations of multiple institutions and/or cohorts with a common objective.'
        "
        :callToAction="
          getSettingValue('CATALOGUE_LANDING_NETWORKS_CTA', data.data._settings)
        "
        :count="data.data.Networks_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/networks/`"
      />
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="image-diagram-2"
        title="Variables"
        :description="
          getSettingValue(
            'CATALOGUE_LANDING_VARIABLES_TEXT',
            data.data._settings
          ) || 'A complete overview of available variables.'
        "
        :callToAction="
          getSettingValue(
            'CATALOGUE_LANDING_VARIABLES_CTA',
            data.data._settings
          )
        "
        :count="data.data.Variables_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/variables/`"
      />
    </div>

    <div
      class="justify-around flex flex-col md:flex-row pt-5 pb-5 lg:pb-10 lg:px-0"
    >
      <LandingCardSecondary icon="people">
        <b>
          {{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Cohorts_agg.sum.numberOfParticipants
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

      <LandingCardSecondary icon="colorize">
        <b
          >{{
            new Intl.NumberFormat("nl-NL").format(
              data.data.Cohorts_agg.sum.numberOfParticipantsWithSamples
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

      <LandingCardSecondary icon="schedule">
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

      <LandingCardSecondary icon="viewTable">
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
    </div>
  </LayoutsLandingPage>
</template>
