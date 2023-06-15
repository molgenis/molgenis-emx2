<template>
  <div>
    <application-header-partial-view />
    <biobank-cards-partial-view />
  </div>
</template>

<script>
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
// import { createBookmark } from '../utils/bookmarkMapper'

export default {
  name: "biobank-explorer",
  components: {
    ApplicationHeaderPartialView,
    BiobankCardsPartialView,
  },
  // mounted () {
  //   /** check if collections have been added off-screen. */
  //   createBookmark(this.filters, this.selectedCollections)
  // }
};
</script>

<style>
.non-commercial .fa-times {
  font-size: 1em;
}

.remove-collection:hover,
.btn:hover,
#select-all-label:hover {
  cursor: pointer;
}
</style>
