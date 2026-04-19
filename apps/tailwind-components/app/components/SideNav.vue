<script setup lang="ts">
import { useRoute } from "#app/composables/router";

interface Section {
  id: string;
  label: string;
}

defineProps<{
  sections: Section[];
  title?: string;
  image?: string;
  headerTarget?: string;
}>();

const route = useRoute();

function activeClass(hash: string) {
  return hash === route.hash
    ? "border-l-4 border-menu-active pl-4 font-bold hover:cursor-pointer"
    : "hover:font-bold hover:cursor-pointer";
}
</script>

<template>
  <nav
    class="text-body-base text-title-contrast bg-content rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary mb-18"
    aria-label="Section navigation"
  >
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <NuxtLink v-if="image" :to="{ ...route, hash: headerTarget }">
        <img :src="image" :alt="title" class="max-w-full" />
      </NuxtLink>
      <NuxtLink
        v-else
        :to="{ ...route, hash: headerTarget }"
        style="
          overflow-wrap: break-word;
          word-wrap: break-word;
          white-space: normal;
        "
      >
        {{ title }}
      </NuxtLink>
    </div>
    <ul>
      <li v-for="section in sections" :key="section.id">
        <ClientOnly>
          <NuxtLink
            class="capitalize w-full block my-2"
            :to="{ ...route, hash: '#' + section.id }"
            :class="activeClass('#' + section.id)"
            :aria-current="route.hash === '#' + section.id ? 'true' : undefined"
          >
            {{ section.label }}
          </NuxtLink>
        </ClientOnly>
      </li>
    </ul>
  </nav>
</template>
