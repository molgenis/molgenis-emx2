<template>
  <Error>
    <div class="main-view">
      <ApplicationHeaderPartialView />
      <BiobankCardsPartialView />
    </div>
  </Error>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import Error from "../components/Error.vue";
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
import { createBookmark } from "../functions/bookmarkMapper";
import { useCheckoutStore } from "../stores/checkoutStore";
import { useFiltersStore } from "../stores/filtersStore";

const checkoutStore = useCheckoutStore();
const filtersStore = useFiltersStore();

onMounted(() => {
  /** mutated cart on another page */
  if (!checkoutStore.checkoutValid) {
    createBookmark(
      filtersStore.filters,
      checkoutStore.selectedCollections,
      checkoutStore.selectedServices
    );
    checkoutStore.checkoutValid = true;
  }
});
</script>

<style>
.main-view {
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  width: 70%;
}

.non-commercial .fa-times {
  font-size: 1em;
}

.remove-collection:hover,
.btn:hover,
#select-all-label:hover {
  cursor: pointer;
}
</style>
