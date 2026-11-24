<script setup lang="ts">
import { definePageMeta } from "#imports";
import { useRoute } from "nuxt/app";
import { computed } from "vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import ButtonBar from "../../../tailwind-components/app/components/ButtonBar.vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Users from "./admin/users.vue";

const ADMIN_SECTIONS = [
  { id: "users", label: "Users", to: "/admin/users" },
  { id: "settings", label: "Settings", to: "/admin/settings" },
] as const;

definePageMeta({
  middleware: "admin-only",
});

const route = useRoute();

const activeSection = computed(
  () =>
    ADMIN_SECTIONS.find((section) => route.path.includes(section.id))?.id ??
    "users"
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
            :type="activeSection === section.id ? 'primary' : 'outline'"
            size="small"
            :aria-current="activeSection === section.id ? 'page' : undefined"
            @click="navigate"
          >
            {{ section.label }}
          </Button>
        </NuxtLink>
      </ButtonBar>
    </nav>
    <Users v-if="activeSection === 'users'" />
    <NuxtPage v-else />
  </Container>
</template>
