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
                <section class="flex flex-col p-4">
                  <div class="mb-1 text-title">Hi {{ session?.email }}</div>
                  <Button size="small" type="primary" @click="handleSignOut"
                    >Sign out</Button
                  >
                </section>
              </div>
            </template>
          </VDropdown>
          <NuxtLink v-else to="/login">
            <HeaderButton label="Signin" icon="user" />
          </NuxtLink>
        </template>
        <template #logo-mobile>
          <LogoMobile link="/" />
        </template>
        <template #nav-mobile>
          <Navigation :navigation="navigation" />
        </template>
      </Header>

      <main class="flex-1">
        <div class="max-w-[1440px] mx-auto px-6 py-6">
          <div
            v-if="!isSignedIn && !isLoginPage"
            class="bg-yellow-200/20 border border-yellow-200/40 text-yellow-800 p-4 rounded-lg"
          >
            Please sign in to access the HPC dashboard.
          </div>
          <div
            v-else-if="isSignedIn && hpcStatus === 'loading'"
            class="text-center py-8 text-body"
          >
            Checking HPC status...
          </div>
          <div
            v-else-if="isSignedIn && hpcStatus === 'not_configured'"
            class="bg-blue-50 border border-blue-200 text-blue-800 p-4 rounded-lg"
          >
            <strong>HPC is disabled.</strong> Set
            <code>MOLGENIS_HPC_ENABLED=true</code> on <code>_SYSTEM_</code> to
            activate the bridge API.
          </div>
          <div
            v-else-if="isSignedIn && hpcStatus === 'unavailable'"
            class="bg-yellow-200/20 border border-yellow-200/40 text-yellow-800 p-4 rounded-lg"
          >
            <strong>HPC health check failed.</strong> Could not reach the HPC
            API. The server may still be starting up.
          </div>
          <slot v-else />
        </div>
      </main>

      <FooterComponent class="mt-[7.8125rem]" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useHead } from "#app";
import { useState } from "#app";
import { computed, ref, watch, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useSession } from "../../../tailwind-components/app/composables/useSession";
import type { ISession } from "../../../tailwind-components/types/types";
import BackgroundGradient from "../../../tailwind-components/app/components/BackgroundGradient.vue";
import Header from "../../../tailwind-components/app/components/Header.vue";
import HeaderButton from "../../../tailwind-components/app/components/HeaderButton.vue";
import Logo from "../../../tailwind-components/app/components/Logo.vue";
import LogoMobile from "../../../tailwind-components/app/components/LogoMobile.vue";
import Navigation from "../../../tailwind-components/app/components/Navigation.vue";
import FooterComponent from "../../../tailwind-components/app/components/FooterComponent.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import { fetchHpcHealth } from "../composables/useHpcApi";

const session = useState<ISession | null>("session", () => null);
const sessionClient = ref<Awaited<ReturnType<typeof useSession>> | null>(null);
const route = useRoute();
const isLoginPage = computed(() => {
  const normalizedPath = route.path.replace(/\/+$/, "") || "/";
  return normalizedPath === "/login";
});

useHead({
  titleTemplate: (titleChunk: string | undefined): string | null => {
    return titleChunk ? `${titleChunk} | HPC Dashboard` : "HPC Dashboard";
  },
});

const isSignedIn = computed(
  () => !!session.value?.email && session.value?.email !== "anonymous"
);

const hpcStatus = ref<"loading" | "ok" | "not_configured" | "unavailable">(
  "loading"
);

async function ensureSessionClient() {
  if (sessionClient.value) return sessionClient.value;
  try {
    sessionClient.value = await useSession();
    return sessionClient.value;
  } catch (e) {
    console.error("Failed to initialize session:", e);
    session.value = null;
    return null;
  }
}

async function handleSignOut() {
  const client = await ensureSessionClient();
  if (!client) {
    return;
  }
  await client.signOut();
}

async function checkHealth() {
  try {
    const health = await fetchHpcHealth();
    if (!health) {
      hpcStatus.value = "unavailable";
    } else if (!health.hpc_enabled) {
      hpcStatus.value = "not_configured";
    } else {
      hpcStatus.value = "ok";
    }
  } catch {
    hpcStatus.value = "unavailable";
  }
}

watch(
  isSignedIn,
  (val) => {
    if (val) checkHealth();
  },
  { immediate: true }
);

const navigation = computed(() => [
  { label: "Jobs", link: "/" },
  { label: "Workers", link: "/workers" },
  { label: "Artifacts", link: "/artifacts" },
]);

onMounted(() => {
  void ensureSessionClient();
});
</script>

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
