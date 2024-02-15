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

<style>
/* removing the built-in nav because it conflicts */
nav[aria-label="breadcrumb"]:not(.directory-nav) {
  display: none;
}
ol.breadcrumb {
  margin-top: 0.25rem !important;
  margin-bottom: 0.25rem !important;
}

/* emx2 style override */
#app > div {
  background-color: white !important;
}
nav.navbar {
  background-color: #e9ecef !important;
}
nav.navbar a.nav-link,
nav.navbar button.btn {
  color: #495057 !important;
  background-color: transparent;
}
nav.navbar a.nav-link:hover,
nav.navbar button.btn:hover {
  color: #ec6707 !important;
}
nav.navbar button.btn.btn-outline-light:not(.border-0) {
  border-color: #495057 !important;
}
nav.navbar button.btn.btn-outline-light:not(.border-0):hover {
  border-color: #ec6707 !important;
  background-color: #ec6707 !important;
  color: #dbedff !important;
}

.filterbar .dropdropdown-button {
  color: #08205c !important;
}
.filterbar .dropdropdown-button:hover {
  color: white !important;
}
</style>
