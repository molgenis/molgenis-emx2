<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();

const { data, pending, error, refresh } = await useFetch(
  `/${route.params.schema}/catalogue/graphql`,
  {
    baseURL: config.public.apiBase,
    method: "POST",
    body: { query: "{Cohorts_agg { count } }" },
  }
);
</script>
<template>
  <LayoutsLandingPage class="w-10/12">
    <PageHeader
    class="mx-auto w-7/12 "
      title="UMCG Research Data Catalogue"
      description="This catalogue contains metadata from cohorts, biobanks and studies of the UMCG. These include a large variety of clinical research data and biological samples available for collaborative research. Work with us for more healthy years and the Future of Health."
    ></PageHeader>

    <div
      class="bg-white shadow-primary flex flex-col px-5 pt-5 pb-6 antialiased lg:pb-10 lg:px-0"
    >
      <div class="flex flex-col items-center text-title">
        <span class="mb-2 mt-2.5 xl:block hidden text-icon">
          <BaseIcon name="image-link" :width="55" />
        </span>
        <div class="relative">
          <h1 class="font-display text-heading-6xl">Cohorts</h1>

     
          <slot v-if="!pending" name="title-suffix">
            <h5 class="text-center">{{ data.data.Cohorts_agg.count }}</h5></slot
          >
        </div>
        <p class="mt-1 mb-0 text-center lg:mb-5 text-body-lg">
          a compete overview of all cohorts and biobanks within the UMCG.
        </p>
        <NuxtLink to="ssr-catalogue/cohorts">
          <Button label="Cohorts" type="secondary" size="medium" />
        </NuxtLink>
      </div>
    </div>
  </LayoutsLandingPage>
</template>
