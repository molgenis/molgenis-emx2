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

<script setup lang="ts">
import { computed } from "vue";
import { useFiltersStore } from "../../stores/filtersStore";

const filtersStore = useFiltersStore();

const props = defineProps<{
  facetIdentifier: string;
  options: () => Promise<Array<{ text: string; value: string }>>;
  trueOption: { text: string; value: string };
}>();

const filterSelection = computed<boolean>({
  get() {
    return filtersStore.getFilterValue(props.facetIdentifier) || false;
  },
  set(value: boolean) {
    filtersStore.updateFilter(props.facetIdentifier, value);
  },
});
</script>

<style scoped>
input {
  accent-color: var(--white);
}
button:hover {
  background-color: var(--secondary) !important;
}
</style>
