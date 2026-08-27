<script setup lang="ts">
import type { MenuItem } from "../../types/types";

const nestedNavigation: MenuItem[] = [
  { label: "Home", link: "#" },
  {
    label: "Data",
    link: "#",
    submenu: [
      { label: "Overview", link: "#overview" },
      {
        label: "Catalog",
        link: "#catalog",
        submenu: [
          { label: "Entities", link: "#entities" },
          { label: "Attributes", link: "#attributes" },
          { label: "Relations", link: "#relations" },
        ],
      },
      {
        label: "Exports",
        link: "#exports",
        submenu: [
          { label: "CSV", link: "#csv" },
          { label: "JSON", link: "#json", isSpaLink: true },
        ],
      },
    ],
  },
  {
    label: "Admin",
    link: "#",
    submenu: [
      { label: "Users", link: "#users" },
      {
        label: "Permissions",
        link: "#permissions",
        submenu: [
          { label: "Roles", link: "#roles" },
          { label: "Policies", link: "#policies" },
        ],
      },
    ],
  },
  { label: "Docs", link: "#docs", isSpaLink: true },
  { label: "Support", link: "#support" },
  {
    label: "More tools",
    link: "#",
    submenu: [
      { label: "Audit", link: "#audit" },
      {
        label: "Integrations",
        link: "#integrations",
        submenu: [
          { label: "SAML", link: "#saml" },
          { label: "OIDC", link: "#oidc" },
        ],
      },
    ],
  },
];
</script>

<template>
  <div class="flex flex-col gap-10 p-6 bg-navigation">
    <section class="flex flex-col gap-4">
      <h2 class="font-display text-heading-xl text-menu">Default Navigation</h2>
      <Navigation :navigation="nestedNavigation" :maximum-button-shown="4" />
    </section>

    <section class="flex flex-col gap-4 p-6 rounded bg-white">
      <h2 class="font-display text-heading-xl">Inverted Navigation</h2>
      <Navigation
        :navigation="nestedNavigation"
        invert
        :maximum-button-shown="3"
      />
    </section>
  </div>
</template>
