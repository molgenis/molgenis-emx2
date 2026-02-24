<script setup lang="ts">
import type { INavigationCards } from "../../../../types/cms";
import ExternalLink from "../../global/icons/ExternalLink.vue";

const props = withDefaults(defineProps<INavigationCards>(), {
  urlIsExternal: false,
});
</script>

<template>
  <div
    :id="id"
    class="grid grid-cols-1 gap-1.5 px-5 py-7.5 text-title-contrast text-center border border-button-tertiary"
  >
    <h3 v-if="title" class="text-title text-heading-lg font-bold">
      {{ title }}
    </h3>
    <p v-if="description" class="text-body-base">{{ description }}</p>
    <a
      :href="url"
      class="flex justify-center items-center gap-1.5 p-2 border rounded-input bg-button-outline text-button-outline border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover duration-default ease-in-out"
    >
      <span v-if="urlLabel">{{ urlLabel }}</span>
      <span v-else>{{ url }}</span>
      <template v-if="urlIsExternal">
        <span class="sr-only">(url opens in a new tab)</span>
        <ExternalLink class="w-6 h-6" />
      </template>
    </a>
  </div>
</template>
