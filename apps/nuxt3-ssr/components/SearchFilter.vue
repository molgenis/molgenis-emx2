<script setup>
const props = defineProps({
  title: {
    type: String,
  },
  filters: {
    type: Array
  }
});

watch(props.filters, (filters) => {
  console.log('filters: ' + JSON.stringify(filters.map(f => f.conditions).filter(f => f.length)))
})
</script>

<template>
  <div class="bg-sidebar-gradient mt-7.5 rounded-t-3px rounded-b-50px">
    <h2 class="p-5 uppercase text-search-filter-title font-display text-heading-3xl">
      {{ title }}
    </h2>

    <template v-if="filters">
      <FilterContainer title="Search in cohorts">
        <FilterSearch />
      </FilterContainer>
      <FilterContainer v-for="filter in filters" :title="filter.title" v-model="filter.conditions">
        <SearchFilterGroup :title="filter.title" :table-name="filter.refTable" v-model="filter.conditions" />
      </FilterContainer>
    </template>

    <slot v-else></slot>

    <hr class="mx-5 border-black opacity-10" />

    <div class="flex items-center p-5">
      <button class="flex items-center">
        <BaseIcon name="plus" class="text-search-filter-expand" :width="18" />
        <span class="ml-3 text-search-filter-expand text-body-base hover:underline">
          More filters
        </span>
      </button>
      <CustomTooltip label="More" hoverColor="white" content="tooltip" class="ml-3" />
    </div>
  </div>
</template>
