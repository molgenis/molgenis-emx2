<script setup lang="ts">
import { useTemplateRef } from "vue";
import type { INavigationCards } from "../../../../types/cms";
import ExternalLink from "../../global/icons/ExternalLink.vue";

const props = withDefaults(defineProps<INavigationCards>(), {
  urlIsExternal: false,
});

const spanEl = useTemplateRef<HTMLSpanElement>("linkText");

function onMouseOver() {
  const offsetWidth = spanEl.value?.offsetWidth as number;
  const scrollWidth = spanEl.value?.scrollWidth as number;
  if (offsetWidth < scrollWidth) {
    (spanEl.value as HTMLSpanElement).title = props.urlLabel || props.url;
  }
}

function onMouseOut() {
  (spanEl.value as HTMLSpanElement).title = "";
}
</script>

<template>
  <div
    :id="id"
    class="relative w-full grid grid-cols-1 gap-1.5 m-auto p-5 mb-2.5 text-title-contrast text-center rounded-base border border-current"
    :class="{
      'sm:w-1/3': navigationCardWidth?.name === 'small',
      'sm:w-1/2': navigationCardWidth?.name === 'medium',
      'sm:w-2/3': navigationCardWidth?.name === 'large',
      'sm:w-full': navigationCardWidth?.name === 'full',
    }"
  >
    <h3 v-if="title" class="text-title text-heading-lg font-bold">
      {{ title }}
    </h3>
    <p v-if="description" class="text-body-base">{{ description }}</p>
    <a
      :href="url"
      class="group flex justify-center items-center gap-1.5 p-2 border border-current rounded-base bg-transparent hover:bg-button-primary hover:text-button-primary duration-default ease-in-out"
      :rel="urlIsExternal ? 'noopener noreferrer' : undefined"
      :target="urlIsExternal ? '_blank' : undefined"
    >
      <span
        ref="linkText"
        class="truncate"
        @mouseover="onMouseOver"
        @mouseout="onMouseOut"
      >
        {{ urlLabel || url }}
      </span>
      <template v-if="urlIsExternal">
        <span class="sr-only">(url opens in a new tab)</span>
        <ExternalLink class="w-6 h-6" />
      </template>
    </a>
    <slot></slot>
  </div>
</template>
