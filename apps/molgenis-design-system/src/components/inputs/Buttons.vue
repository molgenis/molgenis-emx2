<script setup lang="ts">
import { computed } from "vue";

interface ButtonProps {
  label: string,
  type?: 'button' | 'reset' | 'submit',
  context?: 'primary' | 'secondary' | 'outline' | 'none', 
  size?: 'xs' | 'sm' | 'base' | 'lg' | 'xl' | '2xl' | '3xl', 
  isDisabled?: boolean
}

const props = withDefaults(
  defineProps<ButtonProps>(),
  {
    type: 'button',
    context: 'secondary',
    size: 'base',
    isDisabled: false
  }
);

const hasPrimaryHierarchy = computed(() => {
  return ['submit'].includes(props.type) || props.context === 'primary';
})

</script>

<template>
  <button
    :type="type"
    :disabled="isDisabled"
    :class="`
      block
      w-full
      text-center
      ${ 
        hasPrimaryHierarchy
          ? 'bg-blue-800 text-blue-50 hover:bg-blue-700'
          : 'bg-gray-100 text-blue-800 hover:bg-gray-200'
        }
      ${
        isDisabled
        ? 'bg-gray-400 text-gray-600 [&_span]'
        : ''
      }
      rounded-2xl
    `"
  >
  <div class="block w-full px-3 py-3">
      <slot name="button-icon"></slot>
      <span :class="`uppercase font-semibold tracking-widest text-heading-${size}`">
        {{ label }}
      </span>
  </div>
  </button>
</template>
