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

const combobox = useTemplateRef<HTMLDivElement>("combobox");
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
    ref="combobox"
    :aria-required="required"
    class="flex justify-start items-center h-input w-full text-left pl-11 border rounded-input cursor-pointer"
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
    <div class="max-w-[40vw] flex-1 truncate">
      <slot name="ref-dropdown-label"> </slot>
    </div>
    <button
      :id="`${id}-input-toggle`"
      :aria-controls="elemIdControlledByToggle"
      :aria-expanded="isExpanded"
      :aria-haspopup="true"
      @click="onClick()"
      class="mr-4 p-4"
      :class="{
        'cursor-not-allowed': disabled,
      }"
    >
      <BaseIcon
        :width="18"
        name="caret-down"
        class="m-auto transition-all duration-default origin-center"
        :class="{
          'rotate-180': isExpanded,
        }"
      />
      <div class="sr-only">
        <slot name="ref-dropdown-label"></slot>
      </div>
    </button>
  </div>
</template>
