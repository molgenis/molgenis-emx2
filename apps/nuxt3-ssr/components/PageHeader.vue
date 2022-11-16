<script setup>
import BaseIcon from "./BaseIcon.vue";
import { useSlots } from "vue";

const slots = useSlots();

defineProps({
  title: {
    type: String,
    required: true,
  },
  description: { type: String },
  icon: {
    type: String,
  },
});
</script>

<template>
  <header class="flex flex-col pt-5 pb-10 antialiased">
    <div class="mb-6" v-if="slots.prefix">
      <slot name="prefix"></slot>
    </div>
    <div class="flex flex-col items-center text-white">
      <span class="mb-2 mt-2.5" v-if="icon">
        <BaseIcon :name="icon" width="55" />
      </span>
      <div class="relative">
        <h1 class="font-display text-heading-6xl">{{ title }}</h1>

        <div
          class="absolute pl-1 -translate-y-1/2 left-full top-1/2 whitespace-nowrap"
          v-if="slots.title - suffix"
        >
          <slot name="title-suffix"></slot>
        </div>
      </div>
      <p v-if="description" class="mt-1 mb-5 text-center text-body-lg">
        {{ description }}
      </p>
    </div>
    <slot name="suffix"></slot>
  </header>
</template>
