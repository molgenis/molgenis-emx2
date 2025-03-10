<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();
const { initialize } = useGtag();

const datasetStore = useDatasetStore();
await datasetStore.isDatastoreEnabled();

const analyticsService = computed(() => {
  if( typeof config.public.analyticsProvider === "string" ) {
    if(config.public.analyticsProvider.includes("siteimprove") ) {
      return "siteimprove";
    }
    else if(config.public.analyticsProvider === "google-analytics" ) {
      return "google-analytics";
    } else {
      return "";
    }
  }
});

const isAnalyticsAllowedCookie = useCookie("mg_allow_analytics", {
  maxAge: 34560000,
});

const showCookieWall = ref(
  !!(config.public.analyticsKey && isAnalyticsAllowedCookie.value === undefined)
);

function setAnalyticsCookie(value: boolean) {
  isAnalyticsAllowedCookie.value = value.toString();
  showCookieWall.value = false;
  if (value === true) {
    window.location.reload();
  }
}

if(import.meta.client && config.public.analyticsKey && isAnalyticsAllowedCookie.value && analyticsService.value === "google-analytics") {
  initialize(config.public.analyticsKey);
}

const faviconHref = config.public.emx2Theme
  ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
  : "/_nuxt-styles/img/molgenis.ico";
useHead({
  htmlAttrs: {
    'data-theme': route.query.theme as string || config.public.emx2Theme || "",
  },
  link: [
    { rel: "icon", href: faviconHref },
  ],
  titleTemplate: (titleChunk) => {
    if (titleChunk && config.public.siteTitle) {
      return `${titleChunk} | ${config.public.siteTitle}`;
    } else if (titleChunk) {
      return titleChunk;
    } else if (config.public.siteTitle) {
      return config.public.siteTitle;
    } else {
      return "Catalogue";
    }
  },
  script:
    config.public.analyticsKey && isAnalyticsAllowedCookie.value && analyticsService.value === "siteimprove"
      ? [
          {
            src: `https://siteimproveanalytics.com/js/siteanalyze_${config.public.analyticsKey}.js`,
            async: true,
            tagPosition: "bodyClose",
          },
        ]
      : [],
});
</script>

<template>
  {{ datasetStore.isEnabled }}
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">

      <main class="mb-auto">
        <BottomModal
          :show="showCookieWall"
          :full-screen="false"
          button-alignment="right"
        >
          <section
            class="bg-white py-9 lg:px-12.5 px-4 text-gray-900 xl:rounded-3px"
          >
            <h2 class="mb-5 uppercase text-heading-4xl font-display">
              Cookies 🍪🍪🍪
            </h2>
            <div class="mb-5 prose max-w-none">
              We use cookies and similar technologies to enhance your browsing
              experience, analyze website traffic, and personalize content. By
              clicking "Accept," you consent to the use of cookies.
            </div>
            <div class="mb-5 prose max-w-none">
              We value your privacy and are committed to protecting your
              personal information. Our use of cookies is solely for improving
              your experience on our website and ensuring its functionality. We
              do not sell or share your data with third parties.
            </div>
            <div class="flex gap-2">
              <Button @click="setAnalyticsCookie(true)" type="secondary"
                >Accept</Button
              >
              <Button @click="setAnalyticsCookie(false)" type="tertiary"
                >Reject</Button
              >
            </div>
          </section>
        </BottomModal>
        <slot>      
          <NuxtPage />
        </slot>
      </main>

    </div>
  </div>
</template>
