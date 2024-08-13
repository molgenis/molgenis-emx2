<template>
  <section
    class="pb-18 pt-7 pr-16 pl-7 text-gray-900"
    :class="{
      'bg-white': type === 'light',
      'bg-black': type === 'dark',
      'bg-green-500': type === 'success',
      'bg-red-500': type === 'error',
      'bg-yellow-500': type === 'warning',
      'bg-blue-500': type === 'info',
    }"
  >
    <div v-if="subTitle">{{ subTitle }}</div>
    <h2 class="mb-5 uppercase text-heading-4xl font-display" v-if="title">
      {{ title }}
    </h2>
    <div class="mb-5 prose max-w-none" v-if="description">
      <div v-html="description"></div>
    </div>
    <slot></slot>
  </section>
</template>

<script setup lang="ts">
import type { INotificationType } from "~/types/types";

const { title, subTitle, description, type } = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    subTitle?: string;
    type?: INotificationType;
  }>(),
  {
    type: "light",
  }
);
</script>
