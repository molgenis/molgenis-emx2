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
            Organisations:
            {{ biobanksStore.biobankCardsBiobankCount }}
          </td>
        </tr>
        <tr>
          <td class="text-nowrap">
            Collections:
            {{
              biobanksStore.biobankCardsCollectionCount +
              biobanksStore.biobankCardsSubcollectionCount
            }}
          </td>
        </tr>
        <tr>
          <td class="text-nowrap">
            Services: {{ biobanksStore.biobankCardsServicesCount }}
          </td>
        </tr>
      </tbody>
    </table>
  </small>
</template>

<script setup lang="ts">
import { useBiobanksStore } from "../../stores/biobanksStore";

const biobanksStore = useBiobanksStore();
</script>
