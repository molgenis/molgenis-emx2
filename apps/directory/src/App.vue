<script setup>
import { Molgenis } from "molgenis-components";
import { computed, watch } from "vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import { useRoute } from "vue-router";
import { useFiltersStore } from "./stores/filtersStore";
import { useCheckoutStore } from "./stores/checkoutStore";

const route = useRoute();

const query = computed(() => route.query);

const filtersStore = useFiltersStore();
const checkoutStore = useCheckoutStore();

watch(
  query,
  (newQuery) => {
    if (newQuery && Object.keys(newQuery).length) {
      const remainingKeys = Object.keys(newQuery).filter(
        (key) => key !== "cart"
      );
      /** if we only have a cart we do not need to wait for the filters to be applied before updating the biobankcards. */
      if (remainingKeys.length > 0) {
        filtersStore.bookmarkWaitingForApplication = true;
      }
    }

    if (filtersStore.filtersReady && !checkoutStore.cartUpdated) {
      applyBookmark(newQuery);
    }
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
      if (event.target.id) {
        const allDropdownButtons =
          document.querySelectorAll(".dropdown-button");

        for (const dropdownButton of allDropdownButtons) {
          if (dropdownButton.id !== event.target.id) {
            dropdownButton.removeAttribute("open");
          }
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
