<template>
  <StringFilter
    name="Search"
    v-model.lazy="search"
    placeholder="Search"
  ></StringFilter>
</template>

<script>
import StringFilter from "./base/StringFilter.vue";
import { useFiltersStore } from "../../stores/filtersStore";

export default {
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
  components: {
    StringFilter,
  },
  computed: {
    search: {
      get() {
        return this.filtersStore.getFilterValue("search");
      },
      set(value) {
        this.filtersStore.updateFilter("search", value);
      },
    },
  },
};
</script>
