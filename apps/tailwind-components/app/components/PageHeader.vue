<script setup lang="ts">
import { useSlots } from "vue";
import BaseIcon from "./BaseIcon.vue";
import ContentReadMore from "./ContentReadMore.vue";

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
    <div class="flex flex-col text-title">
      <span class="mb-2 mt-2.5 xl:block hidden m-auto" v-if="icon && align === 'center'">
        <BaseIcon :name="icon" :width="55" />
      </span>
      <div class="flex items-center gap-4">
        <div :class="{ 'flex-1': align === 'center' }">
          <div class="ml-auto w-fit gap-4">
            <slot name="title-prefix"></slot>
          </div>
        </div>

        <div class="xl:block hidden" v-if="icon && align === 'left'">
          <BaseIcon :name="icon" :width="55" />
        </div>
        <h1 class="font-display text-heading-6xl">{{ title }}</h1>

        <div class="flex gap-4" :class="{ 'flex-1': align === 'center' }">
          <slot name="title-suffix"></slot>
        </div>
      </div>
      <div
        v-if="slots['description']"
        class="flex mt-1 mb-0 lg:mb-5 text-body-lg"
        :class="{ 'text-center': align === 'center' }"
      >
        <slot name="description"></slot>
      </div>
      <div
        v-if="description"
        class="mt-1 mb-0 lg:mb-5 text-body-lg"
        :class="{ 'text-center': align === 'center' }"
      >
        <ContentReadMore v-if="truncate" :text="description" />
        <p v-else>{{ description }}</p>
      </div>
    </div>
    <slot name="suffix"></slot>
  </header>
</template>
