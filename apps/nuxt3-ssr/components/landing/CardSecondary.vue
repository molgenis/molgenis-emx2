<script setup lang="ts">
defineProps<{
  icon: string;
  count?: number | string;
  title: string;
  link?: string;
}>();
</script>
<template>
  <div
    class="flex flex-col items-start text-title max-w-xs px-5 pt-5 pb-3 antialiased lg:pb-10 lg:px-0"
  >
    <span class="mb-2 mt-2.5 block text-icon">
      <BaseIcon :name="icon" :width="32" class="text-title" />
    </span>
    <p class="mt-1 mb-0 lg:mb-5 text-body-lg">
      <NuxtLink :to="link" :class="{ 'hover:underline': link }">
        <b>
          {{
            typeof count === "number"
              ? new Intl.NumberFormat("nl-NL").format(count)
              : count
          }}
          {{ title }}<br v-if="title" />
        </b>
      </NuxtLink>
      <slot></slot>
    </p>
  </div>
</template>
