<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  count: number;
}>();
</script>
<template>
  <div class="flex flex-col items-center text-title max-w-sm">
    <span class="mb-2 mt-2.5 xl:block hidden text-icon">
      <BaseIcon name="image-link" :width="55" />
    </span>
    <div class="relative">
      <NuxtLink to="ssr-catalogue/cohorts">
        <h1 class="font-display text-heading-6xl">{{ title }}</h1>
      </NuxtLink>
    </div>
    <slot name="title-suffix">
      <span class="bg-blue-50 text-title flex justify-center rounded-full px-3">
        {{ count }}
      </span>
    </slot>
    <p class="mt-1 mb-0 text-center lg:mb-5 text-body-lg">
      {{ description }}
    </p>
    <NuxtLink to="ssr-catalogue/cohorts">
      <Button :label="title" type="secondary" size="medium" />
    </NuxtLink>
  </div>
</template>
