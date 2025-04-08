<template>
  <div class="px-3 pt-1 headerbar card sticky-top border-0 shadow-sm">
    <div class="row my-2">
      <div class="col px-2" aria-label="action-bar">
        <div class="search-container mr-2 mb-2">
          <SearchFilter />
        </div>
      </div>

      <div class="col d-flex filterbar justify-content-end">
        <div>
          <button
            v-if="
              hasActiveFilters &&
              biobanksStore.biobankCardsCollectionCount +
                biobanksStore.biobankCardsSubcollectionCount >
                0
            "
            @click="selectAllCollections"
            type="button"
            class="btn btn-secondary mb-3 text-nowrap"
          >
            Select all collections
            <span class="badge badge-light ml-2">
              {{
                biobanksStore.biobankCardsCollectionCount +
                biobanksStore.biobankCardsSubcollectionCount
              }}</span
            >
          </button>
        </div>
        <div>
          <button
            v-if="
              hasActiveFilters && biobanksStore.biobankCardsServicesCount > 0
            "
            @click="selectAllServices"
            type="button"
            class="btn btn-secondary mb-3 text-nowrap"
          >
            Select all services
            <span class="badge badge-light ml-2">
              {{ biobanksStore.biobankCardsServicesCount }}</span
            >
          </button>
        </div>
        <div>
          <router-link
            v-if="showSettings"
            class="btn btn-light border mr-2 mb-3 text-nowrap"
            to="/configuration"
          >
            <span class="mr-2">Settings</span>
            <span class="fa-solid fa-gear" />
          </router-link>
        </div>
        <check-out :bookmark="true" />
      </div>
    </div>

    <div class="row filterbar p-2" v-if="filtersReady">
      <HtmlDropdown
        v-for="filter in filtersToRender"
        :id="filter.facetIdentifier"
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
        <component
          :is="filter.component"
          v-bind="filter"
          @click="currentFilter = filter.facetIdentifier"
          :currentlyActive="currentFilter === filter.facetIdentifier"
          :optionsFilter="optionsPresent(filter.facetIdentifier)"
        >
        </component>
      </HtmlDropdown>

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
import { HtmlDropdown } from "molgenis-components";
/** */

import CheckOut from "../checkout-components/CheckOut.vue";
import { useBiobanksStore } from "../../stores/biobanksStore";

export default {
  setup() {
    const biobanksStore = useBiobanksStore();
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    const checkoutStore = useCheckoutStore();
    return { biobanksStore, settingsStore, filtersStore, checkoutStore };
  },
  components: {
    SearchFilter,
    HtmlDropdown,
    CheckboxFilter,
    OntologyFilter,
    ToggleFilter,
    CheckOut,
  },
  data() {
    return {
      currentFilter: "",
    };
  },
  computed: {
    optionsPresent() {
      return (facetIdentifier) =>
        this.biobanksStore.getPresentFilterOptions(facetIdentifier);
    },
    hasActiveFilters() {
      return this.filtersStore.hasActiveFilters;
    },
    filtersToRender() {
      if (!this.filtersStore.filtersReadyToRender) return [];

      return this.filtersStore.filterFacets.filter(
        (filterFacet) =>
          filterFacet.showFacet &&
          !filterFacet.builtIn &&
          filterFacet.component !== "ToggleFilter"
      );
    },
    toggleFiltersToRender() {
      if (!this.filtersStore.filtersReadyToRender) return [];

      return this.filtersStore.filterFacets.filter(
        (filterFacet) =>
          filterFacet.showFacet && filterFacet.component === "ToggleFilter"
      );
    },
    showSettings() {
      return this.settingsStore.showSettings;
    },
    filtersReady() {
      return this.filtersStore.filtersReadyToRender;
    },
  },
  methods: {
    clearAllFilters() {
      this.filtersStore.clearAllFilters();
    },
    selectAllServices() {
      const allSelections = this.biobanksStore.biobankCards.map((biobank) => ({
        biobank: { id: biobank.id, name: biobank.name },
        services:
          biobank.services?.map((service) => ({
            label: service.name,
            value: service.id,
          })) || [],
      }));

      const nonEmptyBiobanks = allSelections.filter(
        (item) => item.services.length > 0
      );

      nonEmptyBiobanks.forEach((item) => {
        this.checkoutStore.addServicesToSelection(
          item.biobank,
          item.services,
          true
        );
      });
    },
    selectAllCollections() {
      const allSelections = this.biobanksStore.biobankCards.map((biobank) => ({
        biobank: { id: biobank.id, name: biobank.name },
        collections:
          biobank.collections?.map((collection) => ({
            label: collection.name,
            value: collection.id,
          })) || [],
      }));

      const nonEmptyBiobanks = allSelections.filter(
        (item) => item.collections.length > 0
      );

      nonEmptyBiobanks.forEach((item) => {
        this.checkoutStore.addCollectionsToSelection(
          item.biobank,
          item.collections,
          true
        );
      });
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
  width: 60%;
}
</style>
