<template>
  <span
    class="flex justify-between min-w-80 h-11 pl-2 border-form-required bg-form-required rounded-form-required"
  >
    <span class="my-auto py-1 px-3 text-title">{{ message }}</span>

    <ButtonBar class="rounded-input">
      <button
        v-tooltip.bottom="'previous required field'"
        class="flex items-center border-l border-collapse tracking-widest bg-form-required hover:bg-form-required-btn-hover text-link p-[8px] transition-colors border-l-form-required v-popper--has-tooltip"
        @click="$emit('required-prev')"
      >
        <BaseIcon name="arrow-up" :width="24" />
        <span class="sr-only">previous required field</span>
      </button>
      <button
        v-tooltip.bottom="'next required field'"
        class="flex items-center border-l border-collapse tracking-widest bg-form-required hover:bg-form-required-btn-hover text-link p-[8px] transition-colors border-l-form-required rounded-r-form-required v-popper--has-tooltip"
        @click="$emit('required-next')"
      >
        <BaseIcon name="arrow-down" :width="24" />
        <span class="sr-only">next required field</span>
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
