<template>
  <Molgenis v-model="session" style="background-color: white">
    <template #banner>
      <div v-html="banner" />
    </template>
    <Error />
    <RouterView @click="closeAllDropdownButtons" />
    <template #footer>
      <div v-html="footer" />
    </template>
  </Molgenis>
</template>

<script setup lang="ts">
import { useFavicon, usePreferredDark } from "@vueuse/core";
//@ts-expect-error
import { Molgenis } from "molgenis-components";
import { computed, onMounted, ref, watch } from "vue";
import { LocationQuery, useRoute } from "vue-router";
import Error from "./components/Error.vue";
import { applyBookmark, createBookmark } from "./functions/bookmarkMapper";
import { useCheckoutStore } from "./stores/checkoutStore";
import { useFiltersStore } from "./stores/filtersStore";
import { useSettingsStore } from "./stores/settingsStore";
import router from "./router";

const route = useRoute();
const query = computed(() => route.query);

const filtersStore = useFiltersStore();
const checkoutStore = useCheckoutStore();
const settingsStore = useSettingsStore();

const banner = computed(() => settingsStore.config.banner);
const footer = computed(() => settingsStore.config.footer);

const session = ref({});

watch(
  () => settingsStore.configurationFetched,
  () => {
    if (settingsStore.configurationFetched) {
      initMatomo();
    }
  }
);

watch(session, () => {
  settingsStore.setSessionInformation(session.value);
});

watch(
  query,
  (newQuery: LocationQuery, oldQuery) => {
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
      createBookmark(
        filtersStore.filters,
        checkoutStore.selectedCollections,
        checkoutStore.selectedServices
      );
    }

    if (filtersStore.filtersReady && !checkoutStore.cartUpdated) {
      applyBookmark(newQuery);
    }
  },
  { immediate: true, deep: true }
);

onMounted(changeFavicon);

function closeAllDropdownButtons(event: any) {
  const allDropdownButtons = document.querySelectorAll(".dropdown-button");
  if (event.target?.id) {
    allDropdownButtons.forEach((dropdownButton) => {
      if (dropdownButton.id !== event.target?.id) {
        dropdownButton.removeAttribute("open");
      }
    });
  } else {
    allDropdownButtons.forEach((dropdownButton) => {
      dropdownButton.removeAttribute("open");
    });
  }
}

function changeFavicon() {
  const faviconUrl = getFaviconUrl();
  useFavicon(faviconUrl);
}

function getFaviconUrl() {
  const isDark = usePreferredDark();
  return isDark ? "bbmri-darkmode-favicon.ico" : "bbmri-lightmode-favicon.ico";
}

function initMatomo() {
  const { matomoUrl, matomoSiteId } = settingsStore.config;
  if (matomoUrl && matomoSiteId) {
    const _paq = (window._paq = window._paq || []);
    /* tracker methods like "setCustomDimension" should be called before "trackPageView" */
    _paq.push(["trackPageView"]);
    _paq.push(["enableLinkTracking"]);
    (function () {
      _paq.push(["setTrackerUrl", matomoUrl + "matomo.php"]);
      _paq.push(["setSiteId", matomoSiteId]);
      const doc = document;
      const newScriptElement = doc.createElement("script");
      const firstFoundScriptElement = doc.getElementsByTagName("script")[0];
      newScriptElement.async = true;
      newScriptElement.src = matomoUrl + "matomo.js";
      firstFoundScriptElement.parentNode?.insertBefore(
        newScriptElement,
        firstFoundScriptElement
      );
    })();

    router.afterEach(() => {
      if (window._paq) {
        window._paq.push(["setCustomUrl", window.location.href]);
        window._paq.push(["setDocumentTitle", document.title]);
        window._paq.push(["trackPageView"]);
      }
    });
  } else {
    console.warn(
      `Matomo URL (${matomoUrl}) or Site ID (${matomoSiteId}) is not set in the configuration.`
    );
  }
}
</script>
