<script setup lang="ts">
import { useSlots } from "vue";

const slots: ReturnType<typeof useSlots> = useSlots();

withDefaults(
  defineProps<{
    title: string;
    description?: string;
    icon?: string;
    truncate?: boolean;
    align?: "left" | "center";
  }>(),
  {
    truncate: true,
    align: "center",
  }
);
</script>

<template>
  <header class="flex flex-col px-5 pt-5 pb-6 antialiased lg:pb-10 lg:px-0">
    <div class="mb-6" v-if="slots.prefix">
      <slot name="prefix"></slot>
    </div>
    <div
      class="flex flex-col text-title"
      :class="{ 'items-center': align === 'center' }"
    >
      <span class="mb-2 mt-2.5 xl:block hidden text-icon" v-if="icon">
        <BaseIcon :name="icon" :width="55" />
      </span>
      <div class="relative flex items-center">
        <slot name="title-prefix"></slot>

        <h1 class="font-display text-heading-6xl">{{ title }}</h1>

        <slot name="title-suffix"></slot>
      </div>
      <div
        v-if="slots['description']"
        class="mt-1 mb-0 text-center lg:mb-5 text-body-lg"
      >
        <slot name="description"></slot>
      </div>
      <div
        v-if="description"
        class="mt-1 mb-0 text-center lg:mb-5 text-body-lg"
      >
        <ContentReadMore v-if="truncate" :text="description" />
        <p v-else>{{ description }}</p>
      </div>
    </div>
    <slot name="suffix"></slot>
  </header>
</template>
