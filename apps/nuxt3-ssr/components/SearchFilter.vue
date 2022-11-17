<script setup>
defineProps({
  title: {
    type: String,
  },
});
</script>

<template>
  <div class="bg-sidebar-gradient mt-7.5 rounded-t-3px rounded-b-50px">
    <h2 class="p-5 font-display text-white uppercase text-heading-3xl">
      {{ title }}
    </h2>

    <slot></slot>

    <hr class="mx-5 border-black opacity-10" />

    <div class="flex items-center p-5">
      <a href="#" class="flex items-center">
        <BaseIcon name="plus" class="text-yellow-500" width="18" />
        <span class="text-body-base text-yellow-500 ml-3 hover:underline">
          More filters
        </span>
      </a>
      <Tooltip
        label="Lees meer"
        hoverColor="white"
        content="tooltip"
        class="ml-3"
      />
    </div>
  </div>
</template>
