<script setup type="ts">
const route = useRoute();
const config = useRuntimeConfig();

const cat = route.params.catalogue;
const query = `{
        Networks(filter:{id:{equals:"${cat}"}}) {
              id,
              name,
              description,
              logo {url}
       }
        Variables_agg(filter:{resource:{id:{equals:"${cat}"}}}) {
          count
        }
        Cohorts_agg(filter:{networks:{id:{equals:"${cat}"}}}) {
          count
          sum {
            numberOfParticipants
            numberOfParticipantsWithSamples
          }
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

      }`
console.log(query);
const { data, pending, error, refresh } = await useFetch(
       `/${route.params.schema}/catalogue/graphql`,
       {
              baseURL: config.public.apiBase,
              method: "POST",
              body: { query },
       }
);

//todo, should do same for organisation
console.log(error.value);
const catalogue = data.value.data?.Networks[0];
</script>

<template>
  <LayoutsLandingPage class="w-10/12 pt-8">
    <BreadCrumbs :crumbs="{ catalogues: '../.' }" :current="catalogue.id" />
    <PageHeader
      class="mx-auto lg:w-7/12 text-center"
      :title="catalogue.name"
      :description="catalogue.description"
    />
    <LandingPrimary>
      <div>
        <img
          v-if="catalogue.logo.url"
          class="max-h-11"
          :src="catalogue.logo.url"
        />
      </div>
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="demography"
        title="Cohorts"
        :description="
          'Browse ' +
          catalogue.id +
          ' catalogued population and disease specific cohort studies'
        "
        :count="data.data.Cohorts_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/cat/${catalogue.id}/cohorts`"
      />
      <LandingCardPrimary
        image="clinical"
        title="Data sources"
        :description="
          'Browse ' +
          catalogue.id +
          ' catalogued health and population databanks and registries'
        "
        :count="data.data.DataSources_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/cat/${catalogue.id}/datasources`"
      />
      <LandingCardPrimary
        v-if="!config.public.cohortOnly"
        image="checklist"
        title="Variables"
        :description="'A listing of ' + catalogue.id + ' harmonized variables.'"
        :count="data.data.Variables_agg.count"
        :link="`/${route.params.schema}/ssr-catalogue/cat/${catalogue.id}/variables`"
      />
    </LandingPrimary>
    <LandingSecondary> </LandingSecondary>
  </LayoutsLandingPage>
</template>
