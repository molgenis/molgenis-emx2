<script setup lang="ts">
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
withDefaults(
  defineProps<{
    href: string;
    target?: "_self" | "_blank" | "_parent" | "_top";
  }>(),
  {
    target: "_self",
  }
);
</script>

<template>
  <a
    :href="href"
    :target="target"
    class="text-link underline hover:bg-link-hover"
  >
    <BaseIcon
      v-if="target === '_blank'"
      name="external-link"
      class="inline mr-2"
    />{{ href }}
  </a>
</template>
