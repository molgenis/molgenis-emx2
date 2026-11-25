<script setup lang="ts">
import type { MenuItem } from "../../types/types";

const navigation: MenuItem[] = [
  { label: "Home", location: "#" },
  { label: "About", location: "#" },
  { label: "Contact", location: "#" },
  { label: "Data", location: "#" },
  {
    label: "Help",
    location: "",
    submenu: [
      { label: "Docs", location: "#" },
      { label: "FAQ", location: "#" },
    ],
  },
  { label: "News", location: "#" },
];

const mobileNavigation: MenuItem[] = [
  { label: "Home", location: "#" },
  { label: "Data", location: "#" },
  { label: "Contact", location: "#" },
];
</script>

<template>
  <div>
    <p class="mb-4 text-title">
      Resize the window to see the small (below 768px), medium (768px to 1279px)
      and large (1280px and up) header.
    </p>
    <Header>
      <template #logo>
        <Logo />
      </template>
      <template #nav>
        <Navigation :navigation="navigation" />
      </template>
      <template #logo-mobile>
        <LogoMobile />
      </template>
      <template #nav-mobile>
        <Navigation :navigation="navigation" />
      </template>
      <template #nav-hamburger>
        <HeaderHamburgerMenu :navigation="navigation" />
      </template>
      <template #nav-mobile-row>
        <Navigation :navigation="mobileNavigation" :showMoreButton="false" />
      </template>
    </Header>
  </div>
</template>
