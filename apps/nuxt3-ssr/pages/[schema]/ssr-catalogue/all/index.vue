<script setup lang="ts">
const links = [
  "cohorts",
  "datasources",
  "organisations",
  "variables",
  "networks",
];
</script>
<template>
  <LayoutsSearchPage>
    <template #main>
      <SearchResults>
        <template #header>
          <PageHeader
            title="All resources"
            description="click a resource below to browse"
          >
            <template #prefix>
              <BreadCrumbs :crumbs="{ Home: '.' }" current="browse" />
            </template>
          </PageHeader>
        </template>
        <template #search-results>
          <SearchResultsList>
            <CardList>
              <ReferenceCard
                v-for="link in links"
                :key="link"
                :url="'all/' + link"
                :title="link"
              ></ReferenceCard>
            </CardList>
          </SearchResultsList>
        </template>
      </SearchResults>
    </template>
  </LayoutsSearchPage>
</template>
