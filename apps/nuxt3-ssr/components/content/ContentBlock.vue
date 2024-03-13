<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<template>
  <section
    class="bg-white py-18 lg:px-12.5 px-5 text-gray-900 xl:rounded-3px last:rounded-b-50px last:mb-18 shadow-primary xl:border-b-0 border-b-[1px] overflow-hidden"
  >
    <h2 class="mb-5 uppercase text-heading-4xl font-display" v-if="title">
      {{ title }}
    </h2>
    <div class="mb-5 prose max-w-none" v-if="description">
      <ContentReadMore class="text-body-base" :text="description" />
    </div>
    <slot></slot>
  </section>
</template>
