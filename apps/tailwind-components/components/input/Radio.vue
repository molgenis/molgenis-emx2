<template>
  <input
    v-model="modelValue"
    :value="value"
    type="radio"
    class="w-5 h-5 ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 hover:cursor-pointer"
  />
</template>

<script lang="ts" setup>
defineProps<{
  value: string;
}>();

const modelValue = defineModel<string>("");
</script>
