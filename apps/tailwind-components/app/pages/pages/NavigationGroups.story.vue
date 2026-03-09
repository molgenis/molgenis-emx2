<script setup lang="ts">
import NavigationGroups from "../../components/pages/Navigation/NavigationGroups.vue";

const links = [
  {
    id: "EFDMXMMRhW91",
    title: "Learn more",
    description:
      "Visit our docs to learn more about our software and how to use it",
    url: "https://molgenis.github.io/molgenis-emx2/#/",
    urlLabel: "Read the docs",
    urlIsExternal: true,
  },
  {
    id: "2UoF7nu62PnR",
    title: "Contribute to the code",
    description:
      "Checkout our GitHub repository and contribute to our codebase",
    url: "https://github.com/molgenis/molgenis-emx2",
    urlLabel: "Go to github",
    urlIsExternal: true,
  },
  {
    id: "UKX8WqOxwP9a",
    title: "Contact us",
    description:
      "Send us an email with any questions about the MOLGENIS software",
    url: "mailto:some.email@test.com",
    urlLabel: "Contact",
    urlIsExternal: true,
  },
];
</script>

<template>
  <div class="my-5">
    <NavigationGroups id="navigation-group" :links="links" />
  </div>
</template>
