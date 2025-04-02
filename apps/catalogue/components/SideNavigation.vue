<script setup lang="ts">
import { useRoute } from "#app/composables/router";

const route = useRoute();
defineProps<{
  title?: string;
  image?: string;
  items: { label: string; id: string }[];
  headerTarget?: string;
}>();
function setSideMenuStyle(hash: string) {
  return hash == route.hash
    ? "border-l-4 menu-active pl-4 font-bold hover:cursor-pointer"
    : "hover:font-bold hover:cursor-pointer";
}
</script>

<template>
  <nav
    class="text-body-base bg-white rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary"
  >
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <NuxtLink
        v-if="image"
        :to="{ ...route, hash: headerTarget }"
        :src="image"
      >
        <img :src="image" :alt="title" />
      </NuxtLink>
      <NuxtLink v-else :to="{ ...route, hash: headerTarget }">
        {{ title }}
      </NuxtLink>
    </div>
    <ul>
      <li v-for="item in items">
        <ClientOnly>
          <NuxtLink
            class="capitalize w-full block my-2"
            :to="{ ...route, hash: '#' + item.id }"
            :class="setSideMenuStyle('#' + item.id)"
          >
            {{ item.label }}
          </NuxtLink>
        </ClientOnly>
      </li>
    </ul>
  </nav>
</template>
