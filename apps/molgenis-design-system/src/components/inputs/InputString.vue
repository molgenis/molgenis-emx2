<script setup lang="ts">
import { ref } from "vue";
import { CheckCircleIcon, ExclamationTriangleIcon } from "@heroicons/vue/24/solid";

interface Props {
  id: string,
  type?: 'text' | 'search' | 'number' | 'email' | 'password' | 'tel' | 'url',
  label: string,
  placeholder?: string,
  isDisabled?: boolean,
  isRequired: boolean;
  isValid?: boolean, 
  hasError?: boolean,
  error?:string,
}

const props = withDefaults(
  defineProps<Props>(),
  {
    type: 'text',
    isValid: false,
    isDisabled: false,
    isRequired: false,
    hasError: false,
  }
);

const modelValue = ref("");
</script>

<template>
  <div 
    :class="`
      flex flex-row justify-center items-center w-full border-2 py-2 rounded-md
      ${!isValid && !hasError && !isDisabled ? '[&:first-child]:focus-within:border-gray-600' : ''}
      ${isDisabled ? 'bg-gray-200/10': ''}
      ${hasError ? 'border-red-500 [&_label]:text-red-500 [&_label]:font-semibold [&_svg>path]:fill-red-500' : ''}
      ${isValid ? 'border-green-800 [&_svg>path]:fill-green-800 [&_label]:text-green-800' : ''}
    `"
  >
  <div class="grow flex flex-col-reverse w-5/6">
    <input
      v-model="modelValue"
      :type="type"
      :id="id"
      :placeholder="placeholder"
      :required="isRequired"
      :disabled="isDisabled"
      class="peer grow block w-[90%] py-1 pl-3 ml-2 bg-transparent focus:outline-none"
    />
    <label
      :for="id"
      class="
        block ml-5 grow 
        peer-required:after:content-['*']
        peer-required:after:text-red-500
        peer-disabled:text-gray-400
      "
    >
      {{ label }}
    </label>
  </div>
  <div class="w-1/6 [&_svg]:w-[1.5rem]">
    <component :is="isValid ? CheckCircleIcon : hasError ? ExclamationTriangleIcon : null" class="m-auto" />
  </div>
  </div>
  <div v-if="hasError" class="w-full md:w-[92%] m-auto mt-2">
    <p class="text-red-500 font-semibold">{{ error }}</p>
  </div>
</template>
