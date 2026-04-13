<script setup lang="ts">
import { useHead } from "#app";

import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";

import { useRouter } from "#app";

const router = useRouter();

const pageTitle = "Search all catalogues";
const pageDescription = "Search across all catalogues for variables, cohorts, datasets and more. Use the search bar below

useHead(() => ({
  title: pageTitle,
  meta: [
    {
      name: "description",
      content: pageDescription,
    },
  ],
}));

function handleSearch(query: string) {
  console.log("Search query:", query);
  if (query?.trim()) {
    router.push({ name: "search", query: { q: query.trim() } });
  }
}
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      :title="pageTitle"
      :description="pageDescription"
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        >
          <div class="flex flex-col items-center w-full lg:mt-5">
           
              <SearchBar @submitSearch="handleSearch" id="search-input" class="w-3/5" />
          
          </div>
        </div>
      </template>
    </PageHeader>
    
  </LayoutsLandingPage>
</template>
