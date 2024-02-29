<script setup>
import { onMounted } from "vue";
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
import { useCheckoutStore } from "../stores/checkoutStore";
import { useFiltersStore } from "../stores/filtersStore";
import { createBookmark } from "../functions/bookmarkMapper";

const checkoutStore = useCheckoutStore();
const filtersStore = useFiltersStore();

onMounted(() => {
  /** mutated cart on another page */
  if (!checkoutStore.checkoutValid) {
    createBookmark(filtersStore.filters, checkoutStore.selectedCollections);
    checkoutStore.checkoutValid = true;
  }
});
</script>

<template>
  <div class="main-view">
    <application-header-partial-view />
    <h1 style="text-align: center; font-size: 100px;"><a href="https://www.myinstants.com/media/sounds/allbase.mp3">All your bases are belong to us</a></h1>
    <biobank-cards-partial-view />
  </div>
</template>

<script>
export default {
  name: "biobank-explorer",
  components: {
    ApplicationHeaderPartialView,
    BiobankCardsPartialView,
  },
};
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
