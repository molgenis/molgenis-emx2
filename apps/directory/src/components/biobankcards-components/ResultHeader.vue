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
          <td>{{ biobanksStore.biobankCardsBiobankCount }}</td>
        </tr>
        <tr>
          <td class="pr-1">Collection(s):</td>
          <td>{{ biobanksStore.biobankCardsCollectionCount }}</td>
        </tr>
        <tr v-if="biobanksStore.biobankCardsSubcollectionCount > 0">
          <td class="pr-1">Subcollection(s):</td>
          <td>{{ biobanksStore.biobankCardsSubcollectionCount }}</td>
        </tr>
      </tbody>
    </table>
  </small>
</template>

<script>
import { useBiobanksStore } from "../../stores/biobanksStore";

export default {
  setup() {
    const biobanksStore = useBiobanksStore();
    return { biobanksStore };
  },
};
</script>
