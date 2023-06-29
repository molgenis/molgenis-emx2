<template>
  <div class="mx-1" v-if="notEmpty">
    <div class="d-flex align-items-center flex-wrap">
      <label class="font-weight-bold mr-1">Because you searched for:</label>
      <span
        class="badge badge-info mb-1 mr-1"
        v-for="match in matches"
        :key="match.name"
      >
        {{ match.value.join(", ") }}
      </span>
    </div>
  </div>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore";
export default {
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
  props: {
    viewmodel: {
      type: Object,
      required: true,
    },
  },
  computed: {
    filterInfoDictionary() {
      const filterInfoDictionary = {};
      this.filtersStore.filterFacets.forEach((filter) => {
        filterInfoDictionary[filter.facetIdentifier] = {
          column: filter.column,
          label: filter.facetTitle,
        };
      });
      return filterInfoDictionary;
    },
    activeFilters() {
      return this.filtersStore.filters;
    },
    filterOptionsCache() {
      return this.filtersStore.filterOptionsCache;
    },
    matches() {
      const matches = [];
      const facetIdentifiers = Object.keys(this.activeFilters);
      for (const facetIdentifier of facetIdentifiers) {
        const activeFilterValues = this.activeFilters[facetIdentifier];
        if (!activeFilterValues) {
          continue;
        } /** no need to check further if there are no active filters */

        const filterColumn = this.filterInfoDictionary[facetIdentifier].column;
        const filterLabel = this.filterInfoDictionary[facetIdentifier].label;
        const potentialMatch = this.viewmodel[filterColumn];

        if (!potentialMatch) {
          continue;
        }

        const isArray = Array.isArray(potentialMatch);
        const match = { name: filterLabel, value: [] };

        for (const activeFilterValue of activeFilterValues) {
          /** need to find the correct filter value instead of the name */
          if (!this.filterOptionsCache[facetIdentifier]) {
            continue; /** if the filteroption does not exist */
          }

          const filterOption = this.filterOptionsCache[facetIdentifier].find(
            (fo) => fo.value === activeFilterValue
          );

          if (!filterOption) continue;

          const filterValue = filterOption.value;

          if (
            (isArray &&
              potentialMatch.some(
                (item) => item.id === filterValue
              )) /** if the value is an array */ ||
            (typeof potentialMatch === "object" &&
              filterValue === potentialMatch.id) /** if value is an object */ ||
            filterValue.toString() ===
              potentialMatch.toString() /** if it is a single value */
          ) {
            match.value.push(filterOption.text);
          }
        }

        if (match.value.length > 0) {
          matches.push(match);
        }
      }

      return matches;
    },
    notEmpty() {
      return this.matches.length > 0;
    },
  },
};
</script>
