<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import { computed } from "vue";

// See https://nuxt.com/docs/4.x/api/components/nuxt-link#handling-static-file-and-cross-app-links
const props = withDefaults(
  defineProps<{
    to: string;
    label: string;
    icon?: string;
    type?: "nuxt" | "static" | "external";
  }>(),
  {
    type: "external",
  }
);

const external = computed(() => {
  return props.type === "external";
});
</script>

<template>
  <NuxtLink
    :to="to"
    class="underline"
    :target="external ? '_blank' : '_self'"
    :rel="external ? 'external noopener noreferrer' : ''"
    :external="type !== 'nuxt'"
  >
    <BaseIcon
      class="inline mr-1"
      :name="icon"
      :width="16"
      v-if="icon"
    /><span>{{ label }}</span
    ><BaseIcon class="inline" name="ExternalLink" :width="16" v-if="external" />
  </NuxtLink>
</template>
