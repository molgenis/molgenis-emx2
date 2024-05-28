<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    url: string;
    isExternal?: boolean;
  }>(),
  {
    isExternal: false,
  }
);
</script>

<template>
  <a
    class="border rounded-3px p-5 flex items-center hover:shadow-md transition-shadow"
    :href="url"
    :target="isExternal ? '_blank' : '_self'"
  >
    <div class="flex-grow align-middle">
      <span class="text-blue-500 text-body-base">
        {{ title }}
      </span>
    </div>
    <IconButton v-if="isExternal" icon="external-link" class="text-blue-500" />
    <IconButton v-else icon="arrow-right" class="text-blue-500 rotate-90" />
  </a>
</template>
