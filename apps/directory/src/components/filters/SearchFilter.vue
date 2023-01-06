<template>
  <StringFilter
    name="Search"
    v-model.lazy="search"
    placeholder="Search"
  ></StringFilter>
</template>

<script>
import StringFilter from "../filters/StringFilter.vue";
import { useFiltersStore } from "../../stores/filtersStore";

export default {
  setup() {
    const filterStore = useFiltersStore();
    return { filterStore };
  },
  components: {
    StringFilter,
  },
  computed: {
    search: {
      get() {
        return this.filterStore.getFilterValue("search");
      },
      set(value) {
          this.filterStore.updateFilter("search", value);
      },
    },
  },
};
</script>
