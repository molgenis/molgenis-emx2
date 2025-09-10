<script lang="ts" setup>
withDefaults(
  defineProps<{
    height?: "xs" | "sm" | "md" | "lg" | "xl";
  }>(),
  {
    height: "md",
  }
);
</script>

<template>
  <div
    class="relative flex justify-center items-center"
    :class="{
      'h-5': height === 'xs',
      'h-10': height === 'sm',
      'h-[10rem]': height === 'md',
      'h-[12rem]': height === 'lg',
      'h-[15rem]': height === 'xl',
    }"
  >
    <div
      class="absolute w-full h-full top-0 left-0 bg-input-busy animate-pulse rounded"
    />
    <BaseIcon
      name="progress-activity"
      class="animate-spin text-input w-10 h-10"
    />
  </div>
</template>
