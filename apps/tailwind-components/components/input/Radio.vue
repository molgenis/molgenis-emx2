<template>
  <input
    :value="value"
    v-model="modelValue"
    type="radio"
    class="w-5 h-5 ml-[6px] mr-2.5 mt-0.5 hover:cursor-pointer"
  />
</template>

<script lang="ts" setup>
defineProps<{
  value: string;
}>();

const modelValue = defineModel<string>();
</script>
