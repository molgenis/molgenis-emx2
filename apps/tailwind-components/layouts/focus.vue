<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
const route = useRoute();
const currentRouteName = computed(() => route.name);
</script>
<template>
  <h1>Component: {{ currentRouteName }}</h1>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden pt-15"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <main class="mb-auto">
        <div id="header-place-holder"></div>
        <div class="xl:flex">
          <div class="xl:pl-7.5 grow p-6">
            <slot></slot>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
