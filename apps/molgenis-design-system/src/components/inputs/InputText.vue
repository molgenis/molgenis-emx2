<template>
  <div>
    <label :for="id">
      <span
        :data-required="required"
        :data-valid="valid"
        :data-error="hasError"
        class="
          block
          mb-1
          data-[required='true']:after:content-['*'] 
          data-[required='true']:after:text-[0.9em]
          data-[required='true']:after:text-red-500
          data-[required='true']:after:font-bold
          data-[required='true']:after:ml-1
          
          data-[valid='true']:text-green-800
          data-[error='true']:text-red-500 
        "
      >
        {{ label }}
      </span>
      <slot name="description"></slot>
    </label>
    <div
      :data-disabled="disabled"
      :data-valid="valid"
      :data-error="hasError"
      class="
        flex
        flex-row
        w-full
        items-center
        mt-1
        border
        rounded-md
        
        focus-within:border-gray-600
        data-[valid='true']:border-green-800
        data-[error='true']:border-red-500
        data-[disabled='true']:bg-gray-200/10
      "
    >
      <div class="w-5/6">
        <input
          :id="id"
          :type="type"
          :placeholder="placeholder"
          :required="required"
          :disabled="disabled"
          class="w-full py-2 pl-3 bg-transparent focus:outline-none"
        />
      </div>      
      <div 
        :data-error="hasError"
        :data-valid="valid"
        class="w-1/6 data-[error='true']:text-red-500 data-[valid='true']:text-green-800"
      >
        <component 
          :is="
            valid ? CheckCircleIcon : (hasError ? ExclamationTriangleIcon : null)
          "
          class="m-auto w-[1.2rem]"
        />
      </div>
    </div>
    <div v-if="hasError" class="w-full md:w-[92%] m-auto mt-2">
      <p class="text-red-500">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
} from "@heroicons/vue/24/solid";

interface Props {
  id: string,
  type?: "text" | "search" | "number" | "email" | "password" | "tel" | "url";
  label: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  valid?: boolean;
  hasError?: boolean;
  error?: string;
}

withDefaults(defineProps<Props>(), {
  type: "text",
  disabled: false,
  required: false,
  valid: false,
  hasError: false,
});

</script>