<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    modelValue: string | number;
    options: string[] | number[];
    required?: boolean;
    hasError?: boolean;
    placeholder?: string;
  }>(),
  {
    required: false,
    hasError: false,
    placeholder: "Select an option",
  }
);

const emit = defineEmits(["update:modelValue"]);
</script>

<template>
  <select
    :modelValue="modelValue"
    @change="
      $event.target &&
        $emit('update:modelValue', ($event.target as HTMLSelectElement).value)
    "
    :id="id"
    :required="required"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border border-transparent border-r-8 outline outline-1 outline-select"
    :class="{ 'border-red-500 text-red-500': hasError }"
  >
    <option disabled value="" :selected="modelValue === ''">
      {{ placeholder }}
    </option>
    <option
      v-for="option in options"
      :value="option"
      :selected="modelValue === option"
    >
      {{ option }}
    </option>
  </select>
</template>
