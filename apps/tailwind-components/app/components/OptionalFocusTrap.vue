<template>
  <UseFocusTrap v-if="enabled" v-bind="$props">
    <slot />
  </UseFocusTrap>
  <template v-else v-bind="$props">
    <slot />
  </template>
</template>

<script setup lang="ts">
import { UseFocusTrap } from "@vueuse/integrations/useFocusTrap/component";
const props = withDefaults(
  defineProps<{
    enabled?: boolean;
  }>(),
  {
    enabled: true,
  }
);
</script>
