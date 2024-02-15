<script setup>
import { hash } from ".fingerprint.js";

defineProps(["error"]);

const config = useRuntimeConfig();

let themeFilename = "styles";
if (config.public.emx2Theme) {
  themeFilename += `.${config.public.emx2Theme}`;
}
if (hash) {
  themeFilename += `.${hash}`;
}

const styleHref = `/_nuxt-styles/css/${themeFilename}.css`;
useHead({
  link: [{ rel: "stylesheet", type: "text/css", href: styleHref }],
  titleTemplate: (titleChunk) => {
    return titleChunk
      ? `${titleChunk} | ${config.public.siteTitle}`
      : `${config.public.siteTitle}`;
  },
});
</script>

<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <UIBackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative">
      <main>
        <slot>
          <div class="flex h-screen">
            <div class="m-auto">
              <div class="items-center justify-between hidden xl:flex h-25">
                <Logo />
              </div>
              <div class="font-display text-heading-7xl">
                Oops!, something went wrong.
              </div>
              <div class="py-5 text-heading-7xl">
                Status code: {{ error.statusCode }}
              </div>

              <div v-if="error.message" class="py-5 text-xl">
                <p>Be: {{ error.message }}</p>
                <p v-if="config.public.debug">
                  Details (debug): {{ JSON.stringify(error, null, 2) }}
                </p>
              </div>
              <div class="py-5">
                <Button @click="$router.go(-1)" label="Go back" size="medium" />
              </div>
            </div>
          </div>
        </slot>
      </main>
    </div>
  </div>
</template>
