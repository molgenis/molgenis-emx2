<template>
  <button
    @click="filterSelection = !filterSelection"
    class="btn btn-outline-secondary"
    :class="filterSelection ? 'bg-secondary text-white' : 'bg-white'"
  >
    <input type="checkbox" v-model="filterSelection" />
    {{ trueOption.text }}
  </button>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore";
export default {
  setup() {
    const filtersStore = useFiltersStore();

    return { filtersStore };
  },
  props: {
    facetIdentifier: {
      type: String,
      required: true,
    },
    options: {
      type: [Function],
      required: true,
    },
    trueOption: {
      type: Object,
      required: true,
    },
  },
  computed: {
    filterSelection: {
      get() {
        return this.filtersStore.getFilterValue(this.facetIdentifier) || false;
      },
      set(value) {
        this.filtersStore.updateFilter(this.facetIdentifier, value || null);
      },
    },
  },
};
</script>

<style scoped>
input {
  accent-color: var(--white);
}
button:hover {
  background-color: var(--secondary) !important;
}
</style>
