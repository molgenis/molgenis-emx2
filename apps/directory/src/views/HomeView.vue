<template>
  <div class="main-view">
    <ApplicationHeaderPartialView />
    <BiobankCardsPartialView />
  </div>
</template>

<script setup lang="ts">
import ApplicationHeaderPartialView from "../components/partial-views/ApplicationHeaderPartialView.vue";
import BiobankCardsPartialView from "../components/partial-views/BiobankCardsPartialView.vue";
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
