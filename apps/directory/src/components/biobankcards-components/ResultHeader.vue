<template>
  <small class="d-inline">
    <table class="text-right">
      <thead>
        <tr>
          <th colspan="2">Search results</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td class="pr-1">Organisations:</td>
          <td>{{ filtersStore.biobankCardsBiobankCount }}</td>
        </tr>
        <tr>
          <td class="pr-1">Collection(s):</td>
          <td>{{ filtersStore.biobankCardsCollectionCount }}</td>
        </tr>
        <tr v-if="filtersStore.biobankCardsSubcollectionCount > 0">
          <td class="pr-1">Subcollection(s):</td>
          <td>{{ filtersStore.biobankCardsSubcollectionCount }}</td>
        </tr>
      </tbody>
    </table>
  </small>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore.ts";

export default {
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
};
</script>
