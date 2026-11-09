<script setup lang="ts">
import { definePageMeta } from "#imports";
import { useHead } from "#app";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import ThemeManager from "../../components/theme/ThemeManager.vue";

definePageMeta({
  middleware: "admin-only",
});

useHead({ title: "Theme - Admin - Molgenis" });
</script>

<template>
  <Container class="flex flex-col items-center">
    <ContentBlock
      class="w-full mt-3"
      title="Application theme"
      description="Used everywhere, unless a database sets its own theme."
    >
      <ThemeManager />
    </ContentBlock>
  </Container>
</template>
