<script setup lang="ts">
import { useRuntimeConfig, useHead } from "#app";
import { useRoute, navigateTo } from "#app/composables/router";
import { useSession } from "#imports";
import { computed } from "vue";

const config = useRuntimeConfig();
const route = useRoute();
const { session, reload: reloadSession } = await useSession();

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
const isAdmin = computed(() => session.value?.admin);

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
  if (!schema.value && isAdmin.value) {
    items.push({ label: "Admin", link: `/${schema.value}/admin` });
  }
  return items;
});

async function signout() {
  const { data, error } = await $fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: { query: `mutation { signout { status } }` },
  });

  if (error || data.signout.status !== "SUCCESS") {
    console.error("Error signing out:", error);
    return;
  }

  reloadSession();
}
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

        <template #account>
          <VDropdown
            v-if="isSignedIn"
            aria-id="account-dropdown"
            :distance="3"
            :skidding="4"
            placement="bottom-end"
          >
            <HeaderButton label="Account" icon="user" />
            <template #popper>
              <div
                class="px-[10px] py-[5px] border-theme border-color-theme rounded-theme bg-form"
              >
                <slot name="account-dropdown">
                  <section class="flex flex-col p-4">
                    <div class="mb-1">Hi {{ session?.email }}</div>

                    <Button size="small" type="primary" @click="signout"
                      >Sign out</Button
                    >
                  </section>
                </slot>
              </div>
            </template>
          </VDropdown>
          <HeaderButton
            v-else
            label="Signin"
            icon="user"
            @click="navigateTo({ path: '/login/' })"
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

<style>
.v-popper--theme-dropdown .v-popper__inner {
  background: none;
  border-radius: 0;
  border: 0;
  box-shadow: none;
}

.v-popper__popper--no-positioning {
  position: fixed;
  z-index: 9999;
  top: 0;
  left: 0;
  height: 100%;
  display: flex;
  width: 100%;
}

.v-popper_fullscreen .v-popper__popper--no-positioning {
  width: 100%;
  max-width: none;
}

.v-popper_right .v-popper__popper--no-positioning {
  left: auto;
  right: 0;
}

.v-popper__popper--no-positioning .v-popper__backdrop {
  display: block;
  background: rgba(0 0 0 / 60%);
}

.v-popper__popper--no-positioning .v-popper__wrapper {
  width: 100%;
  pointer-events: auto;
  transition: transform 0.15s ease-out;
}

.v-popper__popper--no-positioning.v-popper__popper--hidden .v-popper__wrapper {
  transform: translateX(-100%);
}
.v-popper_right
  .v-popper__popper--no-positioning.v-popper__popper--hidden
  .v-popper__wrapper {
  transform: translateX(100%);
}
</style>
