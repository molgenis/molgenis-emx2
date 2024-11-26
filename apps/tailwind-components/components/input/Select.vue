<script setup lang="ts">
interface ISelectOptions {
  value: string | number;
  label?: string;
}

withDefaults(
  defineProps<{
    id: string;
    selectOptions: ISelectOptions[];
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

const modelValue = defineModel<string>();
</script>

<template>
  <select
    v-model="modelValue"
    :id="id"
    :required="required"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white rounded-search-input h-10 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border border-transparent border-r-8 outline outline-1 outline-select"
    :class="{ 'border-red-500 text-red-500': hasError }"
  >
    <option disabled value="" :selected="modelValue === ''">
      {{ placeholder }}
    </option>
    <option
      v-for="option in selectOptions"
      :value="option.value"
      :selected="modelValue === option.value"
    >
      <template v-if="option.label">
        {{ option.label }}
      </template>
      <template v-else>
        {{ option.value }}
      </template>
    </option>
  </select>
</template>
