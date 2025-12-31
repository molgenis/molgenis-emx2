<template>
  <StringFilter name="Search" v-model.lazy="search" placeholder="Search" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useFiltersStore } from "../../stores/filtersStore";
import StringFilter from "./base/StringFilter.vue";
const filtersStore = useFiltersStore();

const search = computed({
  get() {
    return filtersStore.filters.search;
  },
  set(value: string) {
    filtersStore.updateFilter("search", value);
  },
});
</script>
