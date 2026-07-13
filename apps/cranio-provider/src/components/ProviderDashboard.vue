<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

import type { RouteLocationNormalizedLoadedGeneric } from "vue-router";

interface CurrentRouteValue extends RouteLocationNormalizedLoadedGeneric {
  href: string;
}

const route = useRouter();
const path = ref<string>((route.currentRoute.value as CurrentRouteValue).href);

onMounted(() => {
  const links = document.querySelectorAll(".navlinks li a");
  links.forEach((link) => {
    if (link.getAttribute("href") === path.value) {
      (link as HTMLAnchorElement).parentElement?.classList.add("link-selected");
    } else {
      link.parentElement?.classList.remove("link-selected");
    }
  });
});
</script>

<template>
  <div class="provider-display">
    <slot></slot>
  </div>
</template>