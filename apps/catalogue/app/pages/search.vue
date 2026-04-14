<script setup lang="ts">
import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Heading from "../../../tailwind-components/app/components/pages/Heading.vue";
import CardList from "../../../tailwind-components/app/components/CardList.vue";
import CardListItem from "../../../tailwind-components/app/components/CardListItem.vue";
import ValueDecimal from "../../../tailwind-components/app/components/value/Decimal.vue";
import SearchBar from "../components/SearchBar.vue";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "#app/composables/router";

const pageTitle = "Search all catalogues";
const pageDescription =
  "Search across all catalogues for variables, cohorts, datasets and more. Use the search bar below";
const route = useRoute();
const router = useRouter();

interface RagResult {
  score: number;
  source: string;
  url: string;
}

const results = ref<RagResult[]>([]);

function handleSearch(query: string) {
  const trimmedQuery = query?.trim();
  console.log("Search query:", trimmedQuery);
  if (trimmedQuery) {
    results.value = [];
    router.push({ query: { search: trimmedQuery } });
    fetchResults(trimmedQuery);
  }
}

async function fetchResults(query: string) {
  const data = await $fetch<Promise<RagResult[]>>("api/rag", {
    query: { search: query },
  }).catch((error) => {
    console.error("Error on rag query:", error);
    throw error;
  });
  console.log("RAG results:", data);
  results.value = data;
}

const groupedResults = computed(() => {
  if (!results.value) return {};
  return results.value.reduce(
    (groups: Record<string, RagResult[]>, result: RagResult) => {
      if (!groups[result.source]) {
        groups[result.source] = [];
      }
      groups[result.source]?.push(result);
      return groups;
    },
    {}
  );
});

const activeTab = ref();

watch(groupedResults, (newVal) => {
  const sources = Object.keys(newVal).sort((a, b) => a.localeCompare(b));
  if (sources.length > 0) {
    console.log("Setting active tab to:", sources[0]);
    activeTab.value = sources[0];
  } else {
    console.log("No results, setting active tab to null");
    activeTab.value = null;
  }
});

if (route && route.query.search) {
  fetchResults(route.query.search as string);
}

const IDLE_CLASS =
  "bg-search-results-view-tabs text-white hover:cursor-default";
const ACTIVE_CLASS =
  "bg-white text-search-results-view-tabs hover:text-search-results-view-tabs-hover";

function linkLabel(url: string): string {
  try {
    const parsedUrl = new URL(url);
    return parsedUrl.pathname.split("/").filter(Boolean).pop() || url;
  } catch (e) {
    return url;
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
            <SearchBar
              @submitSearch="handleSearch"
              id="search-input"
              class="w-3/5"
            />
          </div>
        </div>
      </template>
    </PageHeader>

    <Heading
      id="results-heading"
      v-if="Object.keys(groupedResults).length > 0"
      class="py-4"
    >
      Results for: {{ route.query.search }}
    </Heading>

    <div
      class="flex flex-row justify-start items-center mb-4"
      v-if="Object.keys(groupedResults).length > 0"
    >
      <div class="flex self-center">
        <button
          v-for="entry in Object.entries<RagResult[]>(groupedResults).sort((a, b) => a[0].localeCompare(b[0]))"
          @click="activeTab = entry[0]"
          class="flex items-center pr-5 tracking-widest uppercase first:rounded-l-full last:rounded-r-full h-50px pl-7 font-display text-heading-xl"
          :class="{
            [ACTIVE_CLASS]: activeTab == entry[0],
            [IDLE_CLASS]: activeTab !== entry[0],
          }"
        >
          {{ entry[0] }} - {{ entry[1].length }}
        </button>
      </div>
    </div>
    <CardList
      class="flex flex-col gap-4 mt-4 bg-form border border-theme rounded pl-5"
    >
      <CardListItem
        v-for="result in groupedResults[activeTab]"
        class="flex gap-2 pt-4 last:mb-4 items-center"
      >
        <NuxtLink :href="result.url" class="text-link text-sm">
          {{ linkLabel(result.url) }}
        </NuxtLink>
        <ValueDecimal :data="Number(result.score.toFixed(2))" :precision="2" />
      </CardListItem>
    </CardList>
  </LayoutsLandingPage>
</template>
