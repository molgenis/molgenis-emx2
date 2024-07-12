<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    required?: boolean;
    checked?: boolean
    value?: string;
  }>(),
  {
    required: false,
    checked: false
  }
)

</script>

<template>
  <input
    :id="id"
    text="checkbox"
    :required="required"
    :checked="checked"
    class="w-full font-sans text-black text-gray-300 outline-none"
  />
  
</template>