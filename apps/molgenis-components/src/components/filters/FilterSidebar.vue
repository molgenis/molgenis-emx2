<template>
  <div>
    <div>
      <FilterContainer
        v-for="(filter, index) in visibleFilters"
        :key="filter.name"
        :title="filter.name"
        :conditions="filter.conditions"
      >
        <FilterInput
          :id="'filter-' + filter.name"
          :conditions="filters[index].conditions"
          @updateConditions="handleUpdateFilter(index, $event)"
          :columnType="filter.columnType"
        />
      </FilterContainer>
    </div>
  </div>
</template>

<script>
import FilterContainer from "./FilterContainer.vue";
import FilterInput from "./FilterInput.vue";

export default {
  name: "FilterSidebar",
  components: {
    FilterInput,
    FilterContainer,
  },
  props: {
    filters: Array,
  },
  computed: {
    visibleFilters () {
      return this.filters.filter(column => column.showFilter && column.columnType !== 'HEADING')
    }
  },
  methods: {
    handleUpdateFilter (index, event) {
      let newFilters = [...this.filters]
      newFilters[index].conditions = event
      this.$emit('updateFilters', newFilters)
    }
  }
};
</script>
