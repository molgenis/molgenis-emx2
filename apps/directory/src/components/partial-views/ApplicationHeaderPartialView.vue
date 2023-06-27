<template>
  <div class="px-3 pt-1 headerbar card sticky-top border-0 shadow-sm">
    <div class="row my-2">
      <div class="col-8" aria-label="action-bar">
        <div class="search-container mr-2 mb-2">
          <SearchFilter />
        </div>
      </div>
    </div>

    <div class="row filterbar p-2">
      <ButtonDropdown
        :id="filter.facetIdentifier"
        v-for="filter in filtersToRender"
        :key="filter.facetIdentifier"
        :button-text="filter.facetTitle"
      >
        <component :is="filter.component" v-bind="filter"> </component>
      </ButtonDropdown>

      <toggle-filter
        v-for="toggleFilter of toggleFiltersToRender"
        :key="toggleFilter.name"
        v-bind="toggleFilter"
      />
    </div>
  </div>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore";
import { useSettingsStore } from "../../stores/settingsStore";

/** Components used for filters */
import SearchFilter from "../filters/SearchFilter.vue";
import CheckboxFilter from "../filters/CheckboxFilter.vue";
import OntologyFilter from "../filters/OntologyFilter.vue";
import ToggleFilter from "../filters/ToggleFilter.vue";
import ButtonDropdown from "../micro-components/ButtonDropdown.vue";
/** */

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, filtersStore };
  },
  components: {
    SearchFilter,
    ButtonDropdown,
    CheckboxFilter,
    OntologyFilter,
    ToggleFilter,
  },
  computed: {
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
