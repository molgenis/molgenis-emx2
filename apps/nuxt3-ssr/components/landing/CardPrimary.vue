<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  count: number;
  link: string;
}>();
</script>
<template>
  <div class="flex flex-col items-center max-w-sm">
    <span class="mb-2 mt-2.5 xl:block hidden text-icon">
      <BaseIcon name="image-link" :width="55" />
    </span>
    <div class="relative">
      <NuxtLink :to="link">
        <h1 class="font-display text-heading-6xl text-title-contrast">
          {{ title }}
        </h1>
      </NuxtLink>
    </div>
    <slot name="title-suffix">
      <span
        class="bg-blue-50 text-title-contrast flex justify-center rounded-full px-3"
      >
        {{ count }}
      </span>
    </slot>
    <p
      class="mt-1 mb-0 text-center lg:mb-5 text-body-lg text-sub-title-contrast"
    >
      {{ description }}
    </p>
    <NuxtLink :to="link">
      <Button :label="'Browse ' + title" type="primary" size="medium" />
    </NuxtLink>
  </div>
</template>
