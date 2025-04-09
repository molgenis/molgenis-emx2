<script setup lang="ts">
import { useRuntimeConfig, useHead } from "#app";
import { useRoute, navigateTo } from "#app/composables/router";
import { useSession } from "#imports";
import { computed } from "vue";

const config = useRuntimeConfig();
const route = useRoute();
const { session } = await useSession();

const faviconHref = config.public.emx2Theme
  ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
  : "/_nuxt-styles/img/molgenis.ico";

useHead({
  htmlAttrs: {
    "data-theme":
      (route.query.theme as string) ||
      (config.public.emx2Theme as string) ||
      "",
  },
  link: [{ rel: "icon", href: faviconHref }],
  titleTemplate: (titleChunk: string | undefined): string | null => {
    if (titleChunk && config.public.siteTitle) {
      return `${titleChunk} | ${config.public.siteTitle}`;
    } else if (titleChunk) {
      return titleChunk;
    } else if (config.public.siteTitle) {
      return config.public.siteTitle as string;
    } else {
      return "Emx2";
    }
  },
});

const isSignedIn = computed(
  () => !!session.value?.email && session.value?.email !== "anonymous"
);
const isAdmin = computed(() => session.value?.email === "admin");

const schema = computed(() => route.params.schema as string);

const navigation = computed(() => {
  const items = [
    { label: "Home", link: "#" },
    { label: "About", link: "#" },
    { label: "Contact", link: "#" },
  ];
  if (schema.value && isAdmin.value) {
    items.push({ label: "Analytics", link: `/${schema.value}/analytics` });
  }
  return items;
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
          <Navigation :navigation="navigation" />
        </template>
        <template #admin v-if="isAdmin">
          <HeaderButton
            label="Admin"
            icon="Database"
            @click="navigateTo({ path: '/admin/' })"
          />
        </template>
        <template #account>
          <HeaderButton
            :label="isSignedIn ? 'Account' : 'Signin'"
            icon="user"
            @click="navigateTo({ path: isSignedIn ? '/account/' : '/login/' })"
          />
        </template>
        <template #logo-mobile>
          <LogoMobile link="/" />
        </template>
        <template #nav-mobile>
          <Navigation :navigation="navigation" />
        </template>
      </Header>

      <main class="mb-auto">
        <slot />
      </main>

      <FooterComponent />
    </div>
  </div>
</template>
