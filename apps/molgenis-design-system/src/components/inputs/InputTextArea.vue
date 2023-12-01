<template>
  <div 
    :id="`input-text-area-${id}`"
    :data-required="required"
    :data-disabled="disabled"
    :data-valid="valid"
    :data-error="error !== undefined"
    class="group"
  >
    <div class="flex flex-row flex-nowrap w-full items-center">
      <div class="w-5/6 mb-1">
        <label :for="id">
          <span
            class="
              block
              group-data-[required='true']:after:content-['*']
              group-data-[required='true']:after:text-[0.9em]
              group-data-[required='true']:after:text-red-500
              group-data-[required='true']:after:font-bold
              group-data-[required='true']:after:ml-1
              
              group-data-[valid='true']:text-green-800
              group-data-[error='true']:text-red-500
            "
          >
            {{ label }}
          </span>
          <slot name="description"></slot>
        </label>
      </div>
      <div
        class="
          w-1/6 
          group-data-[error='true']:text-red-500 
        group-data-[valid='true']:text-green-800"
      >
        <component 
          :is="valid ? CheckCircleIcon : (error ? ExclamationTriangleIcon : null)"
          class="m-auto w-[1.2rem]"
        />
      </div>
    </div>
    <textarea
      v-model="modelValue"
      :id="id"
      :rows="rows"
      :required="required"
      :disabled="disabled"
      :placeholder="placeholder"
      class="
        w-full
        border
        mt-2
        p-3
        outline-none
        rounded-md
        focus:border-gray-600
        
        group-data-[disabled='true']:bg-gray-200/30
        group-data-[valid='true']:border-green-800
        group-data-[error='true']:border-red-500
      "
      />
    <div v-if="error" class="w-full mt-2">
      <p class="text-red-500">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
} from "@heroicons/vue/24/solid";

interface Props {
  id: string,
  label: string,
  placeholder?: string,
  rows?: number,
  required?: boolean,
  disabled?: boolean,
  valid?: boolean,
  error?: string
}


withDefaults(
  defineProps<Props>(),
  {
    rows: 2,
    required: false,
    disabled: false,
    valid: false,
  }
);

const modelValue = ref("");
</script>
