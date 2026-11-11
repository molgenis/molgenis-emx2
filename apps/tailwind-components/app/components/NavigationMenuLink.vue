<script setup lang="ts">
import { computed } from "vue";
import type { MenuItem } from "../../types/types";
import { resolveLinkLocation, resolveLinkType } from "../utils/typeUtils";

const props = defineProps<{
  item: MenuItem;
  basePath: string;
  linkClass: string;
}>();

const linkType = computed(() => resolveLinkType(props.item));
const href = computed(() => resolveLinkLocation(props.item, props.basePath));
</script>

<template>
  <NuxtLink
    v-if="linkType === 'spaLink'"
    :to="item.location"
    :class="linkClass"
  >
    {{ item.label }}
  </NuxtLink>
  <a v-else-if="href" :href="href" :class="linkClass">
    {{ item.label }}
  </a>
  <span v-else :class="linkClass">{{ item.label }}</span>
</template>
