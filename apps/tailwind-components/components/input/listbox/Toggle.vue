<template>
  <button
    role="combobox"
    ref="button"
    aria-haspopup="listbox"
    :aria-required="required"
    :aria-expanded="isExpanded"
    class="flex justify-start items-center h-10 w-full text-left pl-11 border bg-input rounded-search-input text-button-input-toggle focus:ring-blue-300"
    :class="{
      'border-disabled text-disabled bg-disabled': disabled,
      'border-invalid text-invalid': hasError,
    }"
  >
    <slot></slot>
    <div class="w-[60px] flex flex-col">
      <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
      <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
    </div>
  </button>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    isExpanded: boolean;
    required?: boolean;
    disabled?: boolean;
    hasError?: boolean;
  }>(),
  {
    isExpanded: false,
    required: false,
    disabled: false,
    hasError: false,
  }
);

const button = useTemplateRef<HTMLButtonElement>("button");
defineExpose({
  button,
});
</script>
