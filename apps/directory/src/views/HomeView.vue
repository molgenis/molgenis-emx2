<script setup>
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
</script>

<template>
  <div>
    <application-header-partial-view />
    <biobank-cards-partial-view />
  </div>
</template>

<script>
export default {
  name: "biobank-explorer",
  components: {
    ApplicationHeaderPartialView,
    BiobankCardsPartialView,
  }
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
