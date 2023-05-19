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
        <AppHeader />
      </slot>
      <main class="mb-auto">
        <div v-if="showCookieBanner"
          class="m-4 text-center lg:mb-5 text-body-lg border-2 border-dashed border-blue-800"
        >
          <p>Do you want to be tracked ?</p>
          
            <div class="flex flex-row justify-center">
              <Button @click="setAnalyticsCookie(true)" type="secondary"
                >Yes</Button
              >
              <Button @click="setAnalyticsCookie(false)" type="tertiary"
                >No</Button
              >
            </div>
        
        </div>
        <slot>
          <NuxtPage />
        </slot>
      </main>
      <slot name="footer">
        <div class="bg-footer p-6">
          <div class="mt-5 mb-0 text-center text-title text-body-lg">
            This database was created using the
            <a
              class="text-body-base text-footer-link hover:underline"
              href="http://molgenis.org"
              >MOLGENIS</a
            >&nbsp;
            <a
              class="text-body-base text-footer-link hover:underline"
              href="http://github.com/molgenis/molgenis-emx2"
            >
              molgenis-emx2
            </a>
            open source software (license:
            <a
              class="text-body-base text-footer-link hover:underline"
              href="https://github.com/molgenis/molgenis-emx2/blob/master/LICENSE"
              >LGPLv3</a
            >).
          </div>
          <div class="mb-0 text-center lg:pb-5 text-title text-body-lg">
            Please cite
            <a
              class="text-body-base text-footer-link hover:underline"
              href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
            >
              Van der Velde et al (2018)</a
            >
            or
            <a href="https://www.ncbi.nlm.nih.gov/pubmed/21210979">
              Swertz et al (2010)</a
            >
            on use.
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackgroundGradient from "./components/BackgroundGradient.vue";
import { hash } from "./utils/fingerprint.js";

const config = useRuntimeConfig();

const isAnalyticsAllowedCookie = useCookie("mg_allow_analytics");
let showCookieBanner = config.public.analyticsKey && isAnalyticsAllowedCookie.value === undefined;

function setAnalyticsCookie(value: boolean) {
  isAnalyticsAllowedCookie.value = value.toString();
  showCookieBanner = false;
  if(value === true) {
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
      ? `${titleChunk} | ${config.siteTitle}`
      : `${config.siteTitle}`;
  },
  script: config.public.analyticsKey && isAnalyticsAllowedCookie.value
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
