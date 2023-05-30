<template>
  <div class="px-3 pt-1 header-bar card sticky-top border-0 shadow-sm">
    <div class="row my-2">
      <div class="col-8" aria-label="action-bar">
        <div class="search-container mr-2 mb-2">
          <SearchFilter />
        </div>
      </div>
    </div>

    <div class="row">
      <!-- @shown="calculateOptions(filter)"
        @hidden="setInactive(filter)" -->
      <ButtonDropdown
        :id="filter.facetIdentifier"
        v-for="filter in filtersToRender"
        :key="filter.facetIdentifier"
        :button-text="filter.facetTitle"
      >
        <!-- v-if="filterLoading !== filter.name"
      
          :value="activeFilters[filter.name]"
          :satisfyAllValue="activeSatisfyAll.includes(filter.name)"
          v-bind="filter"
          @input="(value) => filterChange(filter.name, value)"
          @satisfy-all="
            (satisfyAll) => filterSatisfyAllChange(filter.name, satisfyAll)
          "
          :optionsFilter="filterOptionsOverride[filter.name]"
          :returnTypeAsObject="true"
          :bulkOperation="true" -->

        <component :is="filter.component" v-bind="filter"> </component>
        <!-- <div class="d-inline-block" v-if="filterLoading === filter.name">
          {{ uiText["filter_loading"] }}
          <i class="fa fa-spinner fa-pulse" aria-hidden="true"></i>
        </div> -->
      </ButtonDropdown>
      <!-- <b-dropdown
        :variant="filterVariant(filter.name)"
        @shown="calculateOptions(filter)"
        @hidden="setInactive(filter)"
        v-for="filter in facetsToRender"
        :key="filter.name"
        boundary="window"
        no-flip
        class="mr-2 mb-1 filter-dropdown"
      >
        <template #button-content>
          <span>{{ filter.label || filter.name }}</span>
          <span
            class="badge badge-light border ml-2"
            v-if="filterSelectionCount(filter.name) > 0"
          >
            {{ filterSelectionCount(filter.name) }}</span
          >
        </template>
        <div class="bg-white p-2 dropdown-contents">
          <component
            v-if="filterLoading !== filter.name"
            :is="filter.component"
            :value="activeFilters[filter.name]"
            :satisfyAllValue="activeSatisfyAll.includes(filter.name)"
            v-bind="filter"
            @input="(value) => filterChange(filter.name, value)"
            @satisfy-all="
              (satisfyAll) => filterSatisfyAllChange(filter.name, satisfyAll)
            "
            :optionsFilter="filterOptionsOverride[filter.name]"
            :returnTypeAsObject="true"
            :bulkOperation="true"
          >
          </component>
          <div class="d-inline-block" v-if="filterLoading === filter.name">
            {{ uiText["filter_loading"] }}
            <i class="fa fa-spinner fa-pulse" aria-hidden="true"></i>
          </div>
        </div>
      </b-dropdown> -->
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
    OntologyFilter
  },
  computed: {
    filtersToRender() {
      return this.filtersStore.filterFacets.filter(
        (filterFacet) => !filterFacet.builtIn
      );
    },
  },
};
</script>

<style>
.header-bar {
  background-color: white;
  z-index: 1000;
}
/* ::v-deep span {
  white-space: nowrap;
} */

.search-container {
  display: inline-flex;
  position: relative;
  top: 2px; /* aligning it with the dropwdowns */
  width: 44%;
}
</style>
