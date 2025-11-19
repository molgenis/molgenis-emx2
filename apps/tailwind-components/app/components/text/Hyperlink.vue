<script lang="ts" setup>
import BaseIcon from "~/components/BaseIcon.vue";
import { computed } from "vue";

const props = defineProps<{
    to: string;
    label?: string;
    icon?: string;
    // See https://nuxt.com/docs/4.x/api/components/nuxt-link#handling-static-file-and-cross-app-links
    type: "nuxt" | "static" | "external";
  }>()

function isExternal() {
  return props.type === "external";
}
</script>

<template>
  <NuxtLink
    :to="to"
    class="underline"
    :target="isExternal() ? '_blank' : '_self'"
    :rel="isExternal() ? 'external noopener noreferrer' : ''"
    :external="type !== 'nuxt'"
  >
    <BaseIcon
      class="inline mr-1"
      :name="icon"
      :width="16"
      v-if="icon"
    /><span>{{ label ? label : to }}</span
    ><BaseIcon class="inline" name="ExternalLink" :width="16" v-if="isExternal()" />
  </NuxtLink>
</template>
