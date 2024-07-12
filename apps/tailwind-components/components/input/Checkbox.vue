<script setup lang="ts">
defineProps<{
  id: string;
  value?: string;
}>()

const modelValue = ref<string | boolean>("");

const emit = defineEmits<{
  (e: "change", value: Event): void;
}>()

</script>

<template>
  <input
    :id="id"
    type="checkbox"
    :name="id"
    v-model="modelValue"
    class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 indeterminate:accent-yellow-500 border border-checkbox hover:cursor-pointer"
    @change="$emit('change', $event.target.value)"
  />
  
</template>