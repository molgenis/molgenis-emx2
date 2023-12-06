<template>
  <div 
    :id="`input-select-${id}`"
    :data-required="required"
    :data-disabled="disabled"
    :data-valid="valid"
    :data-error="error !== undefined && error !== ''"
    :data-title-is-heading="title_is_heading"
    class="group"
    >
    <div class="flex flex-row flex-nowrap w-full items-center mb-1">
      <div class="w-5/6">
        <label :for="id">
          <span 
            class="
            block
            mb-1
            
            group-data-[title-is-heading='true']:text-heading-base
            group-data-[title-is-heading='true']:font-semibold
            
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
          flex
          place-content-end
          pr-1
          group-data-[error='true']:text-red-500 
          group-data-[valid='true']:text-green-800
          group-data-[disabled='true']:text-gray-600/50
        "
      >
        <InputStateIcon
          :valid="valid"
          :error="error !== undefined"
          :disabled="disabled"
        />
      </div>
    </div>
    <div 
      class="
        mt-2
        p-1
        border
        rounded-md
        focus-within:border-gray-600
        group-data-[valid='true']:border-green-800
        group-data-[error='true']:border-red-500
        group-data-[disabled='true']:bg-gray-200/50
      "
    >
      <select
        v-model="modelValue"
        :id="id"
        :required="required"
        :aria-required="required"
        :disabled="disabled"
        class="block w-full p-1 pl-4 border-none outline-none bg-transparent"
      >
        <option disabled></option>
        <option
          v-for="row in options"
          :value="option_value ? row[option_value] : row[option_label]"
        >
          {{ row[option_label] }}
        </option>
      </select>
    </div>
    <div v-if="error" class="w-full mt-2">
      <p class="text-red-500">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import InputStateIcon from "../icons/InputStateIcon.vue";
  
interface Props {
  id: string,
  label: string,
  required?: boolean,
  disabled?: boolean,
  valid?: boolean,
  error?: string,
  
  options: any[],
  option_label: string,
  option_value?: string,
  
  title_is_heading?: boolean,
}

defineProps<Props>();
const modelValue = ref("");

</script>