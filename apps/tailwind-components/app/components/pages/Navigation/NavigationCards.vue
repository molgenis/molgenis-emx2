<script setup lang="ts">
import type { INavigationCards } from "../../../../types/cms";
import ExternalLink from "../../global/icons/ExternalLink.vue";

const props = withDefaults(defineProps<INavigationCards>(), {
  urlIsExternal: false,
});
</script>

<template>
  <div
    class="text-title-contrast grid grid-cols-1 gap-1.5 p-5 border rounded text-center"
  >
    <p v-if="title" class="text-title text-heading-lg font-bold">{{ title }}</p>
    <p v-if="description" class="text-body-base">{{ description }}</p>
    <div
      class="p-2 text-heading-lg tracking-widest uppercase rounded-input font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover cursor-pointer"
    >
      <a :href="url" class="flex justify-center items-center gap-1.5">
        <span v-if="urlLabel">{{ urlLabel }}</span>
        <span v-else>{{ url }}</span>
        <template v-if="urlIsExternal">
          <span class="sr-only">(url opens in a new tab)</span>
          <ExternalLink class="w-6 h-6" />
        </template>
      </a>
    </div>
  </div>
</template>
