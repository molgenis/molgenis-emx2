<script setup lang="ts">
import { useRoute } from "#app/composables/router";
import { computed } from "vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Tab from "../../../tailwind-components/app/components/Tab.vue";

const route = useRoute();

const activeTab = computed(() => {
  if (route.path.includes("users")) return "users";
  if (route.path.includes("settings")) return "settings";
  if (route.path.includes("roles")) return "roles";
  return "users";
});
</script>

<template>
  <PageHeader title="Admin Tools" />
  <Container class="flex flex-col items-center">
    <div class="flex flex-nowrap w-full">
      <NuxtLink to="/admin/users">
        <Tab :active="activeTab === 'users'">Users</Tab>
      </NuxtLink>
      <NuxtLink to="/admin/roles">
        <Tab :active="activeTab === 'roles'">Roles</Tab>
      </NuxtLink>
      <NuxtLink to="/admin/settings">
        <Tab :active="activeTab === 'settings'">Settings</Tab>
      </NuxtLink>
    </div>
    <NuxtPage />
  </Container>
</template>
