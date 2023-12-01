<template>
  <div
    :id="`text-input-${id}`"
    :data-disabled="disabled"
    :data-required="required"
    :data-valid="valid"
    :data-error="error !== undefined"
    class="group"
  >
    <label :for="id">
      <span class="
        block mb-1
        group-data-[required='true']:after:content-['*']
        group-data-[required='true']:after:text-[0.9em]
        group-data-[required='true']:after:text-red-500
        group-data-[required='true']:after:font-bold
        group-data-[required='true']:after:ml-1
        
        group-data-[valid='true']:text-green-800
        group-data-[error='true']:text-red-500
      ">
        {{ label }}
      </span>
      <slot name="description"></slot>
    </label>
    <div 
      class="
        flex
        flex-row
        w-full
        items-center
        mt-2
        border
        rounded-md
        
        group-data-[disabled='true']:bg-gray-200/30
        group-data-[valid='true']:border-green-800
        group-data-[error='true']:border-red-500
      "
    >
      <div class="w-5/6">
        <input
          :id="id"
          :type="type"
          :value="value"
          :placeholder="placeholder"
          :required="required"
          :disabled="disabled"
          class="
            w-full
            py-2
            pl-3
            bg-transparent
            focus:outline-none
            group-data-[disabled='true']:text-gray-600/80
          "
        />
      </div>      
      <div 
        :data-error="error !== 'undefined'"
        :data-valid="valid"
        class="w-1/6 
        group-data-[error='true']:text-red-500 
        group-data-[valid='true']:text-green-800
        group-data-[disabled='true']:text-gray-600/50
        "
      >
        <component 
          :is="
            valid ? CheckCircleIcon : (error ? ExclamationTriangleIcon : ( disabled ? NoSymbolIcon : null))
          "
          class="m-auto w-[1.2rem]"
        />
      </div>
    </div>
    <div v-if="error" class="w-full mt-2">
      <p class="text-red-500">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  NoSymbolIcon
} from "@heroicons/vue/24/solid";

interface Props {
  id: string,
  type?: "text" | "search" | "number" | "email" | "password" | "tel" | "url";
  label: string;
  value?: string,
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  valid?: boolean,  
  error?: string;
}

withDefaults(defineProps<Props>(), {
  type: "text",
  disabled: false,
  required: false,
  valid: false
});

</script>