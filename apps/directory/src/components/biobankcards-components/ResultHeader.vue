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
            Organisation(s): {{ biobanksStore.biobankCardsBiobankCount }}
          </td>
          <td class="text-nowrap">
            Collection(s): {{ biobanksStore.biobankCardsCollectionCount }}
          </td>
        </tr>
        <tr>
          <td class="text-nowrap">
            Service(s): {{ biobanksStore.biobankCardsServicesCount }}
          </td>
          <td class="text-nowrap">
            Subcollection(s): {{ biobanksStore.biobankCardsSubcollectionCount }}
          </td>
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
