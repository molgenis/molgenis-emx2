<script setup lang="ts">
const config = useRuntimeConfig();
const route = useRoute();

const faviconHref = config.public.emx2Theme
  ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
  : "/_nuxt-styles/img/molgenis.ico";

useHead({
  htmlAttrs: {
    "data-theme":
      (route.query.theme as string) || config.public.emx2Theme || "",
  },
  link: [{ rel: "icon", href: faviconHref }],
  titleTemplate: (titleChunk) => {
    if (titleChunk && config.public.siteTitle) {
      return `${titleChunk} | ${config.public.siteTitle}`;
    } else if (titleChunk) {
      return titleChunk;
    } else if (config.public.siteTitle) {
      return config.public.siteTitle;
    } else {
      return "Emx2";
    }
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
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <Header>
        <template #logo>
          <Logo link="/" />
        </template>
        <template #nav>
          <Navigation
            :navigation="[
              { label: 'Home', link: '#' },
              { label: 'About', link: '#' },
              { label: 'Contact', link: '#' },
            ]"
          />
        </template>
        <template #logo-mobile>
          <LogoMobile link="/" />
        </template>
        <template #nav-mobile>
          <Navigation
            :navigation="[
              { label: 'Home', link: '#' },
              { label: 'About', link: '#' },
              { label: 'Contact', link: '#' },
            ]"
          />
        </template>
      </Header>

      <main class="mb-auto">
        <slot />
      </main>

      <FooterComponent />
    </div>
  </div>
</template>
