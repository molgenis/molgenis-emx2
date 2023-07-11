<script setup>
import { Molgenis } from "molgenis-components";
import { computed, watch } from "vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import { useRoute } from "vue-router";
import { useFiltersStore } from "./stores/filtersStore";

const route = useRoute();

const query = computed(() => route.query);

const filtersStore = useFiltersStore();

watch(
  query,
  (newQuery, oldQuery) => {
    if (filtersStore.filtersReady) {
      applyBookmark(newQuery, oldQuery);
    }
    // } else if (newQuery && Object.keys(newQuery).length) {
    // /** check if we have even have a query */
    //   filtersStore.bookmarkWaitingForApplication = true;
    //   applyBookmark(newQuery, oldQuery);
    // }
  },
  { immediate: true, deep: true }
);
</script>

<template>
  <molgenis>
    <RouterView @click="closeAllDropdownButtons" />
  </molgenis>
</template>
<script>
export default {
  methods: {
    closeAllDropdownButtons(event) {
      const allDropdownButtons = document.querySelectorAll(".dropdown-button");

      for (const dropdownButton of allDropdownButtons) {
        if (dropdownButton.id !== event.target.id) {
          dropdownButton.removeAttribute("open");
        }
      }
    },
  },
};
</script>

<style>
/* removing the built-in nav because it conflicts */
nav[aria-label="breadcrumb"]:not(.directory-nav) {
  display: none;
}
</style>
