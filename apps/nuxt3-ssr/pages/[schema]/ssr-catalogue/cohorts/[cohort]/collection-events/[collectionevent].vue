<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();

const pageCrumbs: any = {
  Cohorts: `/${route.params.schema}/ssr-catalogue`,
};

pageCrumbs[
  // @ts-ignore
  route.params.cohort
] = `/${route.params.schema}/ssr-catalogue/cohorts/${route.params.cohort}`;
</script>

<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader
        title="collectionEvent?.name"
        description="collectionEvent?.description"
      >
        <template #prefix>
          <BreadCrumbs :crumbs="pageCrumbs" />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <CollectionEventDisplay :id="route.params.collectionevent" />
    </template>
  </LayoutsDetailPage>
</template>
