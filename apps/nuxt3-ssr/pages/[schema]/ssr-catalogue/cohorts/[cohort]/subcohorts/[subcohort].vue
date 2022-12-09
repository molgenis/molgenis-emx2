<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();

const pageCrumbs: any = {
  Cohorts: `/${route.params.schema}/ssr-catalogue`,
};

// @ts-ignore
pageCrumbs[
  route.params.cohort
] = `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}`;
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader title="subcohort?.name" description="subcohort?.description">
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <SubCohortDisplay :id="route.params.subcohort" />
      <!-- todo add count table ( empty in current test set) -->
    </template>
  </LayoutsDetailPage>
</template>
