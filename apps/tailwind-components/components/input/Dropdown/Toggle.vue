<script lang="ts" setup>
import { ref, useTemplateRef } from "vue";
import { BaseIcon } from "#components";

defineProps<{
  id: string;
  elemIdControlledByToggle: string;
  required?: boolean;
  valid?: boolean;
  invalid?: boolean;
  disabled?: boolean;
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

<template>
  <div
    role="combobox"
    :aria-required="required"
    class="h-input w-full text-left pl-11 border rounded-input flex justify-start items-center"
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
  >
    <div class="w-full">
      <!-- show label or inputs -->
      <slot name="ref-dropdown-label"> </slot>
    </div>
    <button
      :id="`${id}-input-toggle`"
      :aria-controls="elemIdControlledByToggle"
      :aria-expanded="isExpanded"
      :aria-haspopup="true"
      @click="onClick()"
      class="mr-4 p-4"
    >
      <BaseIcon :width="18" name="caret-down" class="mx-auto" />
      <div class="sr-only">
        <slot name="ref-dropdown-label"></slot>
      </div>
    </button>
  </div>
</template>
