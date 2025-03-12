<template>
  <span class="flex justify-between min-w-80 h-11 pl-2 border rounded-3px">
    <span class="my-auto py-1 px-3 text-title">{{ message }}</span>

    <ButtonBar>
      <button
        v-tooltip.bottom="'next required field'"
        class="flex items-center border-l border-collapse tracking-widest bg-button-tertiary text-button-tertiary p-[8px] transition-colors border-gray-200 v-popper--has-tooltip"
      >
        <BaseIcon name="arrow-down" :width="24" />
        <span class="sr-only">next required field</span>
      </button>
      <button
        v-tooltip.bottom="'previous required field'"
        class="flex items-center border-l border-collapse tracking-widest bg-button-tertiary text-button-tertiary p-[8px] transition-colors border-gray-200 v-popper--has-tooltip"
      >
        <BaseIcon name="arrow-up" :width="24" />
        <span class="sr-only">previous required field</span>
      </button>
    </ButtonBar>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  message: string;
}>();

defineEmits(["required-prev", "required-next"]);
</script>
