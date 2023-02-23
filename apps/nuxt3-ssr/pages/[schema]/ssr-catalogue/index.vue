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
        Cohorts_agg { 
          count
          sum {
            numberOfParticipants
            numberOfParticipantsWithSamples 
          }
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

  return Math.round((nLongitudinal / total) * 100);
}
</script>
<template>
  <LayoutsLandingPage class="w-10/12 pt-8">
    <PageHeader
      class="mx-auto lg:w-7/12"
      title="UMCG Research Data Catalogue"
      description="This catalogue contains metadata from cohorts, biobanks and studies of the UMCG. These include a large variety of clinical research data and biological samples available for collaborative research. Work with us for more healthy years and the Future of Health."
    ></PageHeader>

    <div
      class="bg-white shadow-primary justify-around flex flex-row px-5 pt-5 pb-6 antialiased lg:pb-10 lg:px-0"
    >
      <LandingCardPrimary
        title="Cohorts"
        description="A compete overview of all cohorts and biobanks within the UMCG."
        :count="data.data.Cohorts_agg.count"
      />
    </div>

    <div
      class="justify-around flex flex-col md:flex-row pt-5 pb-5 lg:pb-10 lg:px-0"
    >
      <LandingCardSecondary icon="people">
        <b> {{ data.data.Cohorts_agg.sum.numberOfParticipants }} Participants</b
        ><br />The cummulative number of participants of all datasets combined.
      </LandingCardSecondary>

      <LandingCardSecondary icon="colorize">
        <b
          >{{
            data.data.Cohorts_agg.sum.numberOfParticipantsWithSamples
          }}
          Samples</b
        >
        <br />The cummulative number of participants with samples collected of
        all datasets combined.
      </LandingCardSecondary>

      <LandingCardSecondary icon="schedule">
        <b
          >Longitudinal
          {{
            percentageLongitudinal(
              data.data.Cohorts_groupBy,
              data.data.Cohorts_agg.count
            )
          }}%</b
        ><br />Percentage of longitudinal datasets. The remaining datasets are
        cross-sectional.
      </LandingCardSecondary>
    </div>
  </LayoutsLandingPage>
</template>
