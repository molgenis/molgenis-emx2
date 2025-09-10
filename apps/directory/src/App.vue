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
import { useRoute } from "vue-router";
import Error from "./components/Error.vue";
import { applyBookmark } from "./functions/bookmarkMapper";
import router from "./router";
import { useFiltersStore } from "./stores/filtersStore";
import { useSettingsStore } from "./stores/settingsStore";

const route = useRoute();
const query = computed(() => route.query);

const filtersStore = useFiltersStore();
const settingsStore = useSettingsStore();

const banner = computed(() => settingsStore.config.banner);
const footer = computed(() => settingsStore.config.footer);

const session = ref({});

window.onpopstate = function () {
  filtersStore.bookmarkWaitingForApplication = true;
  applyBookmark(query.value);
};

watch(session, () => {
  settingsStore.setSessionInformation(session.value);
});

onMounted(async () => {
  filtersStore.bookmarkWaitingForApplication = true;
  await router.isReady();
  applyBookmark(query.value);
  initMatomo();
  changeFavicon();
});

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
