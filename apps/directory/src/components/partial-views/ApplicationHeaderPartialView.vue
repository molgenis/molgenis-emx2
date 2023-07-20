<template>
  <div class="px-3 pt-1 headerbar card sticky-top border-0 shadow-sm">
    <div class="row my-2">
      <div class="col-8" aria-label="action-bar">
        <div class="search-container mr-2 mb-2">
          <SearchFilter />
        </div>
      </div>
      <div class="col-4 d-flex justify-content-end">
        <router-link
          v-if="showSettings"
          class="btn btn-light border mr-2 align-self-start"
          to="/configuration"
        >
          <span class="mr-2">Settings</span>
          <span class="fa-solid fa-gear" />
        </router-link>
        <check-out :bookmark="true" />
      </div>
    </div>

    <div class="row filterbar p-2">
      <ButtonDropdown
        :id="filter.facetIdentifier"
        v-for="filter in filtersToRender"
        :key="filter.facetIdentifier"
        :button-text="filter.facetTitle"
        :active="filterIsActive(filter.facetIdentifier)"
      >
        <template v-slot:counter>
          <span
            class="badge badge-light border mr-2 ml-1"
            v-if="filterSelectionCount(filter.facetIdentifier) > 0"
          >
            {{ filterSelectionCount(filter.facetIdentifier) }}</span
          >
        </template>
        <component :is="filter.component" v-bind="filter"> </component>
      </ButtonDropdown>

      <toggle-filter
        v-for="toggleFilter of toggleFiltersToRender"
        :key="toggleFilter.name"
        v-bind="toggleFilter"
      />

      <button
        v-if="hasActiveFilters"
        @click="clearAllFilters"
        type="button"
        class="btn btn-link"
      >
        Clear all filters
      </button>
    </div>
  </div>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore";
import { useSettingsStore } from "../../stores/settingsStore";
import { useCheckoutStore } from "../../stores/checkoutStore";

/** Components used for filters */
import SearchFilter from "../filters/SearchFilter.vue";
import CheckboxFilter from "../filters/CheckboxFilter.vue";
import OntologyFilter from "../filters/OntologyFilter.vue";
import ToggleFilter from "../filters/ToggleFilter.vue";
import ButtonDropdown from "../micro-components/ButtonDropdown.vue";
/** */

import CheckOut from "../checkout-components/CheckOut.vue";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    const checkoutStore = useCheckoutStore();
    return { settingsStore, filtersStore, checkoutStore };
  },
  components: {
    SearchFilter,
    ButtonDropdown,
    CheckboxFilter,
    OntologyFilter,
    ToggleFilter,
    CheckOut,
  },
  computed: {
    hasActiveFilters() {
      return this.filtersStore.hasActiveFilters;
    },
    filtersToRender() {
      return this.filtersStore.filterFacets.filter(
        (filterFacet) =>
          filterFacet.showFacet &&
          !filterFacet.builtIn &&
          filterFacet.component !== "ToggleFilter"
      );
    },
    toggleFiltersToRender() {
      return this.filtersStore.filterFacets.filter(
        (filterFacet) =>
          filterFacet.showFacet && filterFacet.component === "ToggleFilter"
      );
    },
    showSettings() {
      return this.settingsStore.showSettings;
    },
  },
  methods: {
    clearAllFilters() {
      this.filtersStore.clearAllFilters();
    },
    filterSelectionCount(facetIdentifier) {
      const options = this.filtersStore.filters[facetIdentifier];
      if (!options || !options.length) {
        return 0;
      } else {
        return options.length;
      }
    },
    filterIsActive(facetIdentifier) {
      return this.filtersStore.filters[facetIdentifier] !== undefined;
    },
  },
};
</script>

<style>
.headerbar {
  background-color: white;
  z-index: 1000;
}

.filterbar {
  gap: 0.25rem;
}

.search-container {
  display: inline-flex;
  position: relative;
  top: 2px; /* aligning it with the dropwdowns */
  width: 44%;
}
</style>
