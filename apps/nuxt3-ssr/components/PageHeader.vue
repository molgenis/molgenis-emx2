<script setup>
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
  <header class="flex flex-col pt-5 lg:pb-10 pb-6 antialiased px-5 lg:px-0">
    <div class="mb-6" v-if="slots.prefix">
      <slot name="prefix"></slot>
    </div>
    <div class="flex flex-col items-center text-white">
      <span class="mb-2 mt-2.5 xl:block hidden" v-if="icon">
        <BaseIcon :name="icon" :width="55" />
      </span>
      <div class="relative">
        <h1 class="font-display text-heading-6xl">{{ title }}</h1>

        <div
          class="
            absolute
            pl-1
            -translate-y-1/2
            left-full
            top-1/2
            whitespace-nowrap
            hidden
            xl:block
          "
          :v-if="slots['title-suffix']"
        >
          <slot name="title-suffix"></slot>
        </div>
      </div>
      <p v-if="description" class="mt-1 lg:mb-5 mb-0 text-center text-body-lg">
        {{ description }}
      </p>
    </div>
    <slot name="suffix"></slot>
  </header>
</template>
