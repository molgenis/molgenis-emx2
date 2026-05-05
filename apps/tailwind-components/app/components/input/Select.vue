<script setup lang="ts">
import { computed, useAttrs } from "vue";
import { type IInputProps } from "../../../types/types";
import type { columnValue } from "../../../../metadata-utils/src/types";
import BaseIcon from "../BaseIcon.vue";

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

defineOptions({ inheritAttrs: false });

const attrs = useAttrs();
const rootClass = computed(() => attrs.class);
const selectAttrs = computed(() => {
  const { class: _class, ...rest } = attrs;
  return rest;
});

function optionValue(option: SelectOption): SelectPrimitive {
  return typeof option === "object" ? option.value : option;
}

function optionLabel(option: SelectOption): string {
  return typeof option === "object" ? option.label : String(option);
}
</script>

<template>
  <div class="relative w-full" :class="rootClass">
    <select
      v-bind="selectAttrs"
      :modelValue="modelValue"
      @change="
        $event.target &&
          $emit('update:modelValue', ($event.target as HTMLSelectElement).value)
      "
      :id="id"
      class="w-full h-input pr-9 pl-3 border outline-none rounded-input appearance-none"
      :class="{
        'bg-input border-valid text-valid': valid && !disabled,
        'bg-input border-invalid text-invalid': invalid && !disabled,
        'border-disabled text-disabled bg-disabled cursor-not-allowed':
          disabled,
        'bg-disabled border-valid text-valid cursor-not-allowed':
          valid && disabled,
        'bg-disabled border-invalid text-invalid cursor-not-allowed':
          invalid && disabled,
        'bg-input text-input hover:border-input-hover focus:border-input-focused':
          !disabled && !invalid && !valid,
      }"
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
    <span
      class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3"
      :class="disabled ? 'text-disabled' : 'text-input-description'"
    >
      <BaseIcon name="caret-down" :width="18" />
    </span>
  </div>
</template>
