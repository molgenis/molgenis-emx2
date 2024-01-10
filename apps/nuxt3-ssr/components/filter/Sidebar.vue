<script setup lang="ts">
import type { IFilter, ISearchFilter } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    title: string;
    filters: IFilter[];
    mobileDisplay: boolean;
    
  }>(),
  {
    mobileDisplay: false,
  }
);

watch(props.filters, (filters) => {
  const search = filters.filter((f) => f.config.type === "SEARCH")[0].search;
  const conditions = JSON.stringify(
    filters.filter((f) => f?.conditions?.length).map((f) => f.conditions)
  );
});
</script>

<template>
  <div
    class="mt-7.5 rounded-t-3px rounded-b-50px"
    :class="{ 'bg-sidebar-gradient': !mobileDisplay }"
  >
    <h2
      v-if="!mobileDisplay"
      class="p-5 uppercase font-display text-heading-3xl text-search-filter-title"
    >
      {{ title }}
    </h2>

    <template v-if="filters">
      <FilterContainer
        v-for="filter in filters"
        :title="filter.config.label"
        v-model:conditions="filter.conditions"
        v-model:search="filter.search"
        :mobileDisplay="mobileDisplay"
        :initialCollapsed="filter.config.initialCollapsed"
      >
        <FilterSearch
          v-if="filter.config.type === 'SEARCH'"
          :mobileDisplay="mobileDisplay"
          v-model="(filter as ISearchFilter).search"
        />
        <FilterOntology
          v-else-if="filter.config.type === 'ONTOLOGY'"
          :table-id="filter.config.ontologyTableId"
          :mobileDisplay="mobileDisplay"
          v-model="filter.conditions"
        />
        <FilterList
          v-else-if="filter.config.type === 'REF_ARRAY'"
          :table-id="filter.config.refTableId"
          :key-field="filter.config.refFields?.key"
          :name-field="filter.config.refFields?.name"
          :descriptionField="filter.config.refFields?.description"
          v-model="filter.conditions"
        />
      </FilterContainer>
    </template>

    <slot v-else></slot>

    <hr class="mx-5 border-black opacity-10" />

    <!-- <div class="flex items-center p-5">
      <button class="flex items-center">
        <BaseIcon name="plus" class="text-search-filter-expand" :width="18" />
        <span
          class="ml-3 text-search-filter-expand text-body-base hover:underline"
        >
          More filters
        </span>
      </button>
      <CustomTooltip
        label="More"
        hoverColor="white"
        content="tooltip"
        class="ml-3"
      />
    </div> -->
  </div>
</template>
