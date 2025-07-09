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
    class="h-input w-full text-left pl-11 border rounded-input"
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
    <div>
      <!-- show label or inputs -->
      <slot> </slot>
    </div>
    <button
      :id="`${id}-input-toggle`"
      :aria-controls="elemIdControlledByToggle"
      :aria-expanded="isExpanded"
      :aria-haspopup="true"
      @click="onClick()"
    >
      <BaseIcon :width="18" name="caret-down" class="mx-auto" />
      <slot name="dropdown-label"></slot>
    </button>
  </div>
</template>
