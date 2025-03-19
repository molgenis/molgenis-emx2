<template>
  <button
    role="combobox"
    ref="button"
    aria-haspopup="listbox"
    :aria-required="required"
    :aria-expanded="isExpanded"
    :aria-activedescendant="selectedElementId"
    class="flex justify-start items-center h-10 w-full text-left pl-11 border rounded-input"
    :class="{
      'bg-input border-invalid text-invalid': invalid && !disabled,
      'bg-input border-valid text-valid': valid && !disabled,
      'bg-disabled border-disabled text-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'bg-input text-input hover:border-input-hover focus:border-input-focused':
        !disabled && !invalid && !valid,
    }"
    @click="onClick()"
  >
    <slot></slot>
    <div class="w-[60px] flex flex-col">
      <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
      <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
    </div>
  </button>
</template>

<script lang="ts" setup>
defineProps<{
  required?: boolean;
  valid?: boolean;
  invalid?: boolean;
  disabled?: boolean;
  selectedElementId?: string;
}>();

const isExpanded = ref<boolean>(false);

function onClick() {
  isExpanded.value = !isExpanded.value;
}

const button = useTemplateRef<HTMLButtonElement>("button");
defineExpose({
  button,
  expanded: isExpanded,
});
</script>
