<script setup lang="ts">
import type { MenuItem } from "../../types/types";

const nestedNavigation: MenuItem[] = [
  { label: "Home", location: "#" },
  {
    label: "Data",
    location: "#",
    submenu: [
      { label: "Overview", location: "#overview" },
      {
        label: "Catalog",
        location: "#catalog",
        submenu: [
          { label: "Entities", location: "#entities" },
          { label: "Attributes", location: "#attributes" },
          { label: "Relations", location: "#relations" },
        ],
      },
      {
        label: "Exports",
        location: "#exports",
        submenu: [
          { label: "CSV", location: "#csv" },
          { label: "JSON", location: "#json" },
        ],
      },
    ],
  },
  {
    label: "Admin",
    location: "#",
    submenu: [
      { label: "Users", location: "#users" },
      {
        label: "Permissions",
        location: "#permissions",
        submenu: [
          { label: "Roles", location: "#roles" },
          { label: "Policies", location: "#policies" },
        ],
      },
    ],
  },
  { label: "Docs", location: "#docs" },
  { label: "Support", location: "#support" },
  {
    label: "More tools",
    location: "#",
    submenu: [
      { label: "Audit", location: "#audit" },
      {
        label: "Integrations",
        location: "#integrations",
        submenu: [
          { label: "SAML", location: "#saml" },
          { label: "OIDC", location: "#oidc" },
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
