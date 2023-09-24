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
        Organisations_agg {
          count
        }
        DataResources_agg {
          count
        }
        DataSources_agg {
          count
        }
        Datasets_agg {
          count
        }
        Subcohorts_agg {
          count
        }
        Networks_agg { 
          count
        }
        Models_agg {
          count
        }
        Cohorts_groupBy {
          count 
          design {
            name
          }
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

  return Math.round((nLongitudinal / total) * 100) + "%";
}
</script>
<template>
  <LayoutsLandingPage class="w-10/12 pt-8">
    <PageHeader
      class="mx-auto lg:w-7/12 text-center"
      title="European Health Data & Cohort Networks Catalogue"
      description="Browse and manage metadata for data resources, such as cohorts, registries, biobanks, and multi-center collaborations thereof such as networks, common data models and studies."
    />
    <LandingPrimary>
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="hub"
        title="Catalogues"
        description="Browse selected resources per network, topic, study or organisation."
        :count="data.data.Networks_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/catalogues`"
      />
      <LandingCardPrimary
        image="patient-list"
        title="Resources"
        description="Search in all resources: cohorts, biobanks and databanks"
        :count="data.data.DataResources_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/cohorts/`"
      />
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="checklist"
        title="Variables"
        description="A listing of all collected, harmonized and standard variables."
        :count="data.data.Variables_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/variables/`"
      />
    </LandingPrimary>
    <LandingSecondary>
      <LandingCardSecondary
        icon="demography"
        title="Cohort studies"
        :count="data.data.Cohorts_agg.count"
      />
      <LandingCardSecondary
        icon="database"
        title="Databanks"
        :count="data.data.DataSources_agg.count"
      />
      <LandingCardSecondary
        icon="hub"
        title="Networks"
        :count="data.data.Networks_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/networks/`"
      />
      <LandingCardSecondary
        icon="institution"
        title="Organisations"
        :count="data.data.Organisations_agg.count"
      />
      <LandingCardSecondary
        icon="dataset"
        title="Datasets"
        :count="data.data.Cohorts_agg.count"
      />
      <LandingCardSecondary
        icon="list"
        title="Collected variables"
        :count="data.data.Networks_agg.count"
      />
      <LandingCardSecondary
        icon="harmonized-variables"
        title="Harmonized variables"
        :count="data.data.Organisations_agg.count"
      />
      <LandingCardSecondary
        icon="dataset-linked"
        title="Standards"
        :count="data.data.Models_agg.count"
      />
      <LandingCardSecondary
        icon="person"
        title="Individuals"
        :count="data.data.Cohorts_agg.sum.numberOfParticipants"
      >
        {{
          "The cumulative number of participants of all (sub)cohorts combined."
        }}
      </LandingCardSecondary>
      <LandingCardSecondary
        icon="colorize"
        title="Samples"
        :count="data.data.Cohorts_agg.sum.numberOfParticipantsWithSamples"
      >
        {{
          "The cumulative number of participants with samples collected of all (sub)cohorts combined"
        }}
      </LandingCardSecondary>
      <LandingCardSecondary
        icon="schedule"
        title="Longitudinal"
        :count="
          percentageLongitudinal(
            data.data.Cohorts_groupBy,
            data.data.Cohorts_agg.count
          )
        "
      >
        {{ "Percentage of longitudinal datasets." }}
      </LandingCardSecondary>
      <LandingCardSecondary
        icon="people"
        title="Subcohorts"
        :count="data.data.Subcohorts_agg.count"
      >
        {{ "Percentage of longitudinal datasets." }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
