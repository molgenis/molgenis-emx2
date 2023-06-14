<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  subTitle?: string;
}>();
</script>

<template>
  <section class="bg-white py-18 lg:px-12.5 px-5 text-gray-900">
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
