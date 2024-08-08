<template>
  <input
    v-model="modelValue"
    type="checkbox"
    class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 indeterminate:accent-yellow-500 border border-checkbox hover:cursor-pointer"
  />
</template>

<script setup lang="ts">
const modelValue = defineModel<boolean | string>();
</script>
