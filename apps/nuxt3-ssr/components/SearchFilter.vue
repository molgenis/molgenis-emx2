<script setup>
defineProps({
  title: {
    type: String,
  },
});
</script>

<template>
  <div class="bg-sidebar-gradient mt-7.5 rounded-t-3px rounded-b-50px">
    <h2 class="p-5 uppercase text-search-filter-title font-display text-heading-3xl">
      {{ title }}
    </h2>

    <slot></slot>

    <hr class="mx-5 border-black opacity-10" />

    <div class="flex items-center p-5">
      <button class="flex items-center">
        <BaseIcon name="plus" class="text-search-filter-expand" :width="18" />
        <span class="ml-3 text-search-filter-expand text-body-base hover:underline">
          More filters
        </span>
      </button>
      <CustomTooltip label="More" hoverColor="white" content="tooltip" class="ml-3" />
    </div>
  </div>
</template>
