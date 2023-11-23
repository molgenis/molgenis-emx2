<script setup lang="ts">
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

</script>

<template>
  <button
    :type="type"
    :data-type="type"
    :disabled="isDisabled"    
    :class="`block w-full text-center rounded-full
      
    data-[type='button']:bg-gray-100 
    data-[type='button']:text-blue-800 
    data-[type='button']:hover:bg-gray-200
      
    data-[type='submit']:bg-blue-800 
    data-[type='submit']:text-blue-50 
    data-[type='submit']:hover:bg-blue-700
      
    [&:disabled]:bg-gray-400
    [&:disabled]:text-gray-600 
    [&:disabled]:hover:bg-gray-400
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
