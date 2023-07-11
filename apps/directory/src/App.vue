<script setup>
import { Molgenis } from "molgenis-components";
import { computed, watch } from "vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import { useRoute } from "vue-router";
import { useFiltersStore } from "./stores/filtersStore";
import { useCheckoutStore} from "./stores/checkoutStore"

const route = useRoute();

const query = computed(() => route.query);

const filtersStore = useFiltersStore();
const checkoutStore = useCheckoutStore();

watch(
  query,
  (newQuery) => {
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
