<template>
  <Molgenis v-model="session">
    <RouterView @click="closeAllDropdownButtons" />
  </Molgenis>
</template>

<script setup>
import { Molgenis } from "molgenis-components";
import { computed, onMounted, watch } from "vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import { useRoute } from "vue-router";
import { useFiltersStore } from "./stores/filtersStore";
import { useCheckoutStore } from "./stores/checkoutStore";
import { useSettingsStore } from "./stores/settingsStore";

const route = useRoute();

const query = computed(() => route.query);

const filtersStore = useFiltersStore();
const checkoutStore = useCheckoutStore();

watch(
  query,
  (newQuery, oldQuery) => {
    if (newQuery && Object.keys(newQuery).length) {
      const remainingKeys = Object.keys(newQuery).filter(
        (key) => key !== "cart"
      );
      /** if we only have a cart we do not need to wait for the filters to be applied before updating the biobankcards. */
      if (remainingKeys.length > 0) {
        filtersStore.bookmarkWaitingForApplication = true;
      }
    } else if (
      oldQuery &&
      Object.keys(oldQuery).length > 0 &&
      newQuery &&
      Object.keys(newQuery).length === 0
    ) {
      filtersStore.clearAllFilters();
      applyBookmark(newQuery);
    }

    if (filtersStore.filtersReady && !checkoutStore.cartUpdated) {
      applyBookmark(newQuery);
    }
  },
  { immediate: true, deep: true }
);
onMounted(async () => {
  const settingsStore = useSettingsStore();
  await settingsStore.initializeConfig();
});

function closeAllDropdownButtons(event) {
  const allDropdownButtons = document.querySelectorAll(".dropdown-button");
  if (event.target.id) {
    for (const dropdownButton of allDropdownButtons) {
      if (dropdownButton.id !== event.target.id) {
        dropdownButton.removeAttribute("open");
      }
    }
  } else {
    for (const dropdownButton of allDropdownButtons) {
      dropdownButton.removeAttribute("open");
    }
  }
}
</script>

<script>
export default {
  data() {
    return {
      session: {},
    };
  },
  watch: {
    session(sessionState) {
      const settingsStore = useSettingsStore();
      settingsStore.setSessionInformation(sessionState);
    },
  },
};
</script>
