<template>
  <div
    class="px-3 pt-1 header-bar card sticky-top border-0 shadow-sm"
  >
    <div class="row my-2">
      <div class="col-8" aria-label="action-bar">
        <div class="search-container mr-2 mb-2">
          <search-filter />
        </div>
        <!-- <b-button
          v-if="numberOfActiveFilters > 0"
          class="mr-2"
          variant="outline-danger"
          @click="ClearActiveFilters">Clear all filters</b-button> -->
      </div>
    </div>
  </div>
</template>

<script>
import { useSettingsStore } from "../../stores/settingsStore";

/** Components used for filters */
import SearchFilter from "../filters/SearchFilter.vue";
/** */

export default {
  setup() {
    const settingsStore = useSettingsStore();
    return { settingsStore };
  },
  components: {
    SearchFilter,
  },
};
</script>

<style>
.header-bar {
  background-color: white;
  z-index: 1000;
}
::v-deep span {
  white-space: nowrap;
}

.dropdown-contents {
  max-width: 50rem;
  overflow: auto;
}

.search-container {
  display: inline-flex;
  position: relative;
  top: 2px; /* aligning it with the dropwdowns */
  width: 44%;
}

/* Theme override */
.dropdown-toggle {
  padding-right: 1.5rem;
}

.filter-dropdown .dropdown-toggle::after {
  content: "";
  position: absolute;
  border-bottom: 0;
  border-left: 0.4rem solid transparent;
  border-left-color: transparent;
  border-left-style: solid;
  border-left-width: 0.4rem;
  border-right: 0.4rem solid transparent;
  border-top: 0.4rem solid;
  content: "";
  display: inline-block;
  margin-left: 0.4rem;
  margin-top: 0.6rem;
}
</style>
