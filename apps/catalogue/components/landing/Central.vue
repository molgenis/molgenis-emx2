<script setup lang="ts">
import { useRuntimeConfig, useFetch } from "#app";

const config = useRuntimeConfig();
const schema = config.public.schema as string;

const { data, pending, error, refresh } = await useFetch(`/${schema}/graphql`, {
  method: "POST",
  body: {
    query: `{
        Variables_agg {
          count
        }
        Resources_agg {
          count
          _sum {
            numberOfParticipants
            numberOfParticipantsWithSamples 
          }
        }
        Resources_groupBy {
          count 
          type {
            name
          }
        }
      }`,
  },
});

function percentageLongitudinal(
  resourcesGroupBy: { count: number; design: { name: string } }[],
  total: number
) {
  const nLongitudinal = resourcesGroupBy.reduce(
    (accum, group) =>
      group?.design?.name === "Longitudinal" ? accum + group.count : accum,
    0
  );

  return Math.round((nLongitudinal / total) * 100) + "%";
}
</script>
<template>
  <LayoutsLandingPage>
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
        :link="`/catalogues`"
      />
      <LandingCardPrimary
        image="patient-list"
        title="Resources"
        description="Browse in all resources: cohorts, biobanks and databanks"
        :count="data.data.DataResources_agg.count"
        :link="`/all`"
      />
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="checklist"
        title="Variables"
        description="A listing of all collected, harmonised and standard variables."
        :count="data.data.Variables_agg.count"
        :link="`/all/variables`"
      />
    </LandingPrimary>
    <LandingSecondary>
      <LandingCardSecondary
        icon="demography"
        title="Cohort studies"
        :count="data.data.Cohorts_agg.count"
        :link="`/all/cohorts`"
      />
      <LandingCardSecondary
        icon="database"
        title="Data sources"
        :count="data.data.DataSources_agg.count"
        :link="`/all/datasources`"
      />
      <LandingCardSecondary
        icon="hub"
        title="Networks"
        :count="data.data.Networks_agg.count"
        :link="`/all/networks`"
      />
      <LandingCardSecondary
        icon="institution"
        title="Organisations"
        :count="data.data.Organisations_agg.count"
        :link="`/all/organisations`"
      />
      <LandingCardSecondary
        icon="dataset"
        title="Datasets"
        :count="data.data.Cohorts_agg.count"
        :link="`/all/datasets`"
      />
      <LandingCardSecondary
        icon="list"
        title="Collected variables"
        :count="data.data.Networks_agg.count"
        :link="`/all/variables`"
      />
      <!-- todo must split in collected and harmonised -->
      <LandingCardSecondary
        icon="harmonized-variables"
        title="Harmonised variables"
        :count="data.data.Organisations_agg.count"
        :link="`/all/variables`"
      />
      <LandingCardSecondary
        icon="dataset-linked"
        title="Standards"
        :count="data.data.Models_agg.count"
        :link="`/all/models`"
      />
      <LandingCardSecondary
        icon="person"
        title="Individuals"
        :count="data.data.Cohorts_agg._sum.numberOfParticipants"
      >
        {{
          "The cumulative number of participants of all (sub)cohorts combined."
        }}
      </LandingCardSecondary>
      <LandingCardSecondary
        icon="colorize"
        title="Samples"
        :count="data.data.Cohorts_agg._sum.numberOfParticipantsWithSamples"
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
        title="Cohorts"
        :count="data.data.Subpopopulations_agg.count"
      >
        {{ "Percentage of longitudinal datasets." }}
      </LandingCardSecondary>
    </LandingSecondary>
  </LayoutsLandingPage>
</template>
