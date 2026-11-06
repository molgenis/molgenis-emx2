<script setup lang="ts">
import { computed } from "vue";
import type { MenuItem } from "../../types/types";
import { resolveLinkType } from "../utils/typeUtils";

const props = defineProps<{
  item: MenuItem;
  basePath: string;
  linkClass: string;
}>();

const linkType = computed(() => resolveLinkType(props.item));
</script>

<template>
  <NuxtLink v-if="linkType === 'spaLink'" :to="item.link" :class="linkClass">
    {{ item.label }}
  </NuxtLink>
  <a
    v-else-if="linkType === 'externalLink'"
    :href="item.link"
    :class="linkClass"
  >
    {{ item.label }}
  </a>
  <a v-else :href="basePath + item.link" :class="linkClass">
    {{ item.label }}
  </a>
</template>
