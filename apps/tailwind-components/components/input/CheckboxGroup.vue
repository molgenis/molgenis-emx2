<template>
  <div :id="`${id}-checkbox-group`">
    <div class="flex flex-row" v-for="option in options">
      <input
        type="checkbox"
        :id="`${id}-${option.value}`"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        :checked="modelValue!.includes(option.value)"
        @input="toggleSelect"
        class="sr-only"
      />
      <InputLabel
        :for="`${id}-${option.value}`"
        class="hover:cursor-pointer flex justify-start items-center"
        :class="{
          'border-invalid text-invalid': error,
          'border-valid text-valid': valid,
          'border-disabled text-disabled bg-disabled': disabled,
        }"
      >
        <InputCheckboxIcon
          :checked="modelValue!.includes(option.value)"
          :error="error"
          :valid="valid"
        />
        <span class="block" v-if="option.label">
          {{ option.label }}
        </span>
        <span class="block" v-else>
          {{ option.value }}
        </span>
      </InputLabel>
    </div>
    <div class="mt-2" v-if="showClearButton">
      <button
        type="reset"
        :id="`${id}-checkbox-group-clear`"
        :form="`${id}-checkbox-group`"
        @click.prevent="resetModelValue"
      >
        Clear
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {
  type InputProps,
  InputPropsDefaults,
  type IValueLabel,
} from "~/types/types";

withDefaults(
  defineProps<
    InputProps & {
      options: IValueLabel[];
      showClearButton?: boolean;
    }
  >(),
  {
    ...InputPropsDefaults,
    showClearButton: false,
  }
);

const modelValue = defineModel<string[]>();
const emit = defineEmits(["update:modelValue", "select", "deselect"]);

function toggleSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.checked) {
    emit("select", target.value);
  } else {
    emit("deselect", target.value);
  }
}

function resetModelValue() {
  modelValue.value = [];
}
</script>
