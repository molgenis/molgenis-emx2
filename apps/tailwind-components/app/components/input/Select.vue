<script setup lang="ts">
import { type IInputProps } from "../../../types/types";
import type { columnValue } from "../../../../metadata-utils/src/types";

type SelectPrimitive = string | number;
type SelectOption = SelectPrimitive | { value: SelectPrimitive; label: string };

const modelValue = defineModel<columnValue>();
withDefaults(
  defineProps<
    IInputProps & {
      options: SelectOption[];
    }
  >(),
  {
    placeholder: "Select an option",
  }
);

const emit = defineEmits(["update:modelValue", "focus"]);

function optionValue(option: SelectOption): SelectPrimitive {
  return typeof option === "object" ? option.value : option;
}

function optionLabel(option: SelectOption): string {
  return typeof option === "object" ? option.label : String(option);
}
</script>

<template>
  <select
    :modelValue="modelValue"
    @change="
      $event.target &&
        $emit('update:modelValue', ($event.target as HTMLSelectElement).value)
    "
    :id="id"
    class="w-full pr-16 font-sans text-black text-gray-300 bg-white rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border border-transparent border-r-8 outline outline-1 outline-select"
    :class="{ 'border-red-500 text-red-500': invalid }"
  >
    <option value="" :selected="modelValue === ''">
      {{ placeholder }}
    </option>
    <option
      v-for="option in options"
      :key="optionValue(option)"
      :value="optionValue(option)"
      :selected="modelValue === optionValue(option)"
    >
      {{ optionLabel(option) }}
    </option>
  </select>
</template>
