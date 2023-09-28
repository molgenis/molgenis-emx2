<script setup lang="ts">
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
    class="text-blue-500 underline hover:bg-blue-50"
  >
    <BaseIcon
      v-if="target === '_blank'"
      name="external-link"
      class="inline mr-2"
    />{{ href }}
  </a>
</template>
