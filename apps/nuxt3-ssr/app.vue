<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <slot name="header">
        <HeaderCatalogue v-if="route.params.catalogue" />
        <HeaderGlobal v-else />
      </slot>
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
      <slot name="footer">
        <div class="bg-footer p-6">
          <div class="mt-5 mb-0 text-center text-title text-body-lg">
            This database was created using
            <a
              class="text-body-base text-footer-link hover:underline"
              href="http://molgenis.org"
              >MOLGENIS.org</a>
            (<a
              class="text-body-base text-footer-link hover:underline"
              href="http://github.com/molgenis/molgenis-emx2"
            >
              github </a
            >).
          </div>
          <div class="mb-0 text-center lg:pb-5 text-title text-body-lg">
            Please cite
            <a
                class="text-body-base text-footer-link hover:underline"
                href="https://www.ncbi.nlm.nih.gov/pubmed/32243569"
            >
              Swertz & Gini (2022)
            </a>,
            <a
              class="text-body-base text-footer-link hover:underline"
              href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
            >
              Van der Velde et al (2018)
            </a>
            or
            <a
              class="text-body-base text-footer-link hover:underline"
              href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"
            >
              Swertz et al (2010)
            </a>
            on use.
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackgroundGradient from "./components/BackgroundGradient.vue";
import { hash } from ".fingerprint.js";

const config = useRuntimeConfig();
const route = useRoute();

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

let themeFilename = "styles";
if (config.public.emx2Theme) {
  themeFilename += `.${config.public.emx2Theme}`;
}
if (hash) {
  themeFilename += `.${hash}`;
}

const styleHref = `/_nuxt-styles/css/${themeFilename}.css`;
const faviconHref = config.public.emx2Theme
  ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
  : "/_nuxt-styles/img/molgenis.ico";
useHead({
  link: [
    { rel: "icon", href: faviconHref },
    { rel: "stylesheet", type: "text/css", href: styleHref },
  ],
  titleTemplate: (titleChunk) => {
    return titleChunk
      ? `${titleChunk} | ${config.public.siteTitle}`
      : `${config.public.siteTitle}`;
  },
  script:
    config.public.analyticsKey && isAnalyticsAllowedCookie.value
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
