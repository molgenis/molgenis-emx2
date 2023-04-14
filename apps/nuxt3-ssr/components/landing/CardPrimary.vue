<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  count: number;
  link: string;
  image: string;
}>();
</script>
<template>
  <div class="flex flex-row md:flex-col items-center max-w-sm">
    <span class="mb-2 mt-2.5 xl:block hidden text-icon">
      <BaseIcon :name="image" :width="55" />
    </span>
    <div class="relative">
      <NuxtLink :to="link">
        <h1
          class="font-display md:text-heading-6xl text-heading-3xl text-title-contrast"
        >
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
      class="mt-1 mb-0 md:block hidden text-center lg:mb-5 text-body-lg text-sub-title-contrast h-24"
    >
      {{ description }}
    </p>

    <NuxtLink class="xl:block hidden" :to="link">
      <Button :label="title" type="primary" size="medium" />
    </NuxtLink>
  </div>
</template>
