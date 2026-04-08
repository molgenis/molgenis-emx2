<script setup lang="ts">
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import InputSearch from "../../../tailwind-components/app/components/input/Search.vue";

import { computed, ref, watch } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Tab from "../../../tailwind-components/app/components/Tab.vue";
import ValueHyperlink from "../../../tailwind-components/app/components/value/Hyperlink.vue";
import ValueDecimal from "../../../tailwind-components/app/components/value/Decimal.vue";
import CardList from "../../../tailwind-components/app/components/CardList.vue";
import CardListItem from "../../../tailwind-components/app/components/CardListItem.vue";
import Heading from "../../../tailwind-components/app/components/pages/Heading.vue";

const query = ref("");
const results = ref();

interface RagResult {
  score: number;
  source: string;
  url: string;
}

async function handleSearch() {
  const data = await $fetch<Promise<RagResult[]>>("/api/rag", {
    query: { search: query.value },
  }).catch((error) => {
    console.error("Error on rag query:", error);
  });
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
  const sources = Object.keys(newVal);
  if (sources.length > 0) {
    activeTab.value = sources[0];
  } else {
    activeTab.value = null;
  }
});
</script>
<template>
  <Container>
    <PageHeader title="Rag demo" />

    <div class="flex flex-row justify-center items-center mb-4">
      <InputSearch
        class="w-1/2"
        v-model="query"
        placeholder="Type a search query and press enter to search"
        id="search-input"
      />
      <Button class="ml-2" @click="handleSearch">Search</Button>
    </div>

    <Heading
      id="results-heading"
      v-if="Object.keys(groupedResults).length > 0"
      class="py-4"
      >Results by type</Heading
    >

    <div
      class="flex flex-row justify-start items-center mb-4 border-b"
      v-if="Object.keys(groupedResults).length > 0"
    >
      <Tab
        v-for="entry in Object.entries<RagResult[]>(groupedResults)"
        :active="activeTab === entry[0]"
        @click="activeTab = entry[0]"
        class="capitalize"
      >
        {{ entry[0] }} ({{ entry[1].length }})
      </Tab>
    </div>
    <CardList class="flex flex-col gap-4 mt-4">
      <CardListItem
        v-for="result in groupedResults[activeTab]"
        :key="result.id"
        class="flex gap-2 pt-4"
      >
        <ValueHyperlink :data="result.url" />
        <ValueDecimal :data="result.score" :precision="2" />
      </CardListItem>
    </CardList>
  </Container>
</template>
