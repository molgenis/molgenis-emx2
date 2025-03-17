<template>
  <small>
    <table class="table table-sm table-borderless">
      <thead>
        <tr>
          <th colspan="2">Search results</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td class="text-nowrap">
            {{ organisationLabel }}:
            {{ biobanksStore.biobankCardsBiobankCount }}
          </td>
        </tr>
        <tr>
          <td class="text-nowrap">
            {{ collectionLabel }}:
            {{
              biobanksStore.biobankCardsCollectionCount +
              biobanksStore.biobankCardsSubcollectionCount
            }}
          </td>
        </tr>
        <tr>
          <td class="text-nowrap">
            {{ serviveLabel }}: {{ biobanksStore.biobankCardsServicesCount }}
          </td>
        </tr>
      </tbody>
    </table>
  </small>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useBiobanksStore } from "../../stores/biobanksStore";

const biobanksStore = useBiobanksStore();

const organisationLabel = computed(() => {
  return biobanksStore.biobankCardsBiobankCount > 1
    ? "Organisations"
    : "Organisation";
});

const collectionLabel = computed(() => {
  return biobanksStore.biobankCardsCollectionCount > 1
    ? "Collections"
    : "Collection";
});

const serviveLabel = computed(() => {
  return biobanksStore.biobankCardsServicesCount > 1 ? "Services" : "Service";
});
</script>
