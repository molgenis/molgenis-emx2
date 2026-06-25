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
          <Logo link="/" :image="logoUrl" />
        </template>
        <template #nav>
          <Navigation :navigation="userMenuItems" />
        </template>

        <template #account>
          <AccountMenu
            :has-custom-content="hasAccountDropdownSlot"
            :isSignedIn="isSignedIn"
            :email="session?.email"
            @signOut="signOut"
          >
            <slot name="account-dropdown" />
          </AccountMenu>
        </template>
        <template #logo-mobile>
          <LogoMobile link="/" :image="logoUrl" />
        </template>
        <template #nav-mobile>
          <Navigation :navigation="menuItems" />
        </template>
      </Header>

      <main>
        <slot />
      </main>

      <FooterComponent class="mt-[7.8125rem]">
        <FooterVersion />
      </FooterComponent>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from "vue";
import BackgroundGradient from "../../../tailwind-components/app/components/BackgroundGradient.vue";
import Header from "../../../tailwind-components/app/components/Header.vue";
import Logo from "../../../tailwind-components/app/components/Logo.vue";
import LogoMobile from "../../../tailwind-components/app/components/LogoMobile.vue";
import Navigation from "../../../tailwind-components/app/components/Navigation.vue";
import FooterComponent from "../../../tailwind-components/app/components/FooterComponent.vue";
import FooterVersion from "../../../tailwind-components/app/components/FooterVersion.vue";
import AccountMenu from "../components/AccountMenu.vue";
import { useLayoutState } from "../composables/useLayoutState";

const { isSignedIn, logoUrl, menuItems, session, signOut, userMenuItems } =
  await useLayoutState();
const slots = useSlots();
const hasAccountDropdownSlot = computed(() => !!slots["account-dropdown"]);
</script>
