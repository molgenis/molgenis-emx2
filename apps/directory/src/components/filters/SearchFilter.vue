<template>
  <StringFilter
    name="Search"
    v-model="search"
    placeholder="Search"
  ></StringFilter>
</template>

<script>
import StringFilter from "../filters/StringFilter.vue";
import { useFiltersStore } from "../../stores/filtersStore";
import { storeToRefs } from 'pinia';

export default {
  setup() {
    const filterStore = useFiltersStore();

    const { search } = storeToRefs(filterStore);
    return { search };
  },
  components: {
    StringFilter,
  },
};
</script>
