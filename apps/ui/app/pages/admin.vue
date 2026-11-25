<script setup lang="ts">
import { definePageMeta } from "#imports";
import { useRoute } from "nuxt/app";
import { computed } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import ButtonBar from "../../../tailwind-components/app/components/ButtonBar.vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import ContentBlock from "../../../tailwind-components/app/components/content/ContentBlock.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Users from "./admin/users.vue";

const ADMIN_SECTIONS = [
  {
    id: "users",
    label: "Users",
    to: "/admin/users",
    title: "User management",
    description: "",
  },
  {
    id: "roles",
    label: "Roles",
    to: "/admin/roles",
    title: "Role management",
    description: "",
  },
  {
    id: "settings",
    label: "Settings",
    to: "/admin/settings",
    title: "Settings management",
    description: "",
  },
] as const;

definePageMeta({
  middleware: "admin-only",
});

const route = useRoute();

const activeSection = computed(
  () =>
    ADMIN_SECTIONS.find((section) => route.path.includes(section.id)) ??
    ADMIN_SECTIONS[0]
);
</script>

<template>
  <Container>
    <PageHeader title="Admin Tools" />
    <nav aria-label="Admin sections" class="mb-3 px-5 lg:px-0">
      <ButtonBar>
        <NuxtLink
          v-for="section in ADMIN_SECTIONS"
          :key="section.id"
          :to="section.to"
          custom
          v-slot="{ href, navigate }"
        >
          <Button
            :id="`admin-section-${section.id}`"
            :href="href ?? undefined"
            :type="activeSection.id === section.id ? 'primary' : 'outline'"
            size="small"
            :aria-current="activeSection.id === section.id ? 'page' : undefined"
            @click="navigate"
          >
            {{ section.label }}
          </Button>
        </NuxtLink>
      </ButtonBar>
    </nav>
    <ContentBlock
      class="w-full"
      :title="activeSection.title"
      :description="activeSection.description"
    >
      <Users v-if="activeSection.id === 'users'" />
      <NuxtPage v-else />
    </ContentBlock>
  </Container>
</template>
