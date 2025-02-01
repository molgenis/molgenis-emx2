<template>
  <div
    :id="`${id}-radio-group`"
    class="flex gap-1"
    :class="{
      'flex-row': align === 'horizontal',
      'flex-col': align === 'vertical',
      'border-invalid text-invalid': error,
      'border-valid text-valid': valid,
      'border-disabled bg-disabled': disabled,
    }"
  >
    <div v-for="option in options" class="flex justify-start align-center">
      <InputRadio
        :id="`${id}-radio-group-${option.value}`"
        class="sr-only fixed"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        @input="toggleSelect"
        :checked="option.value === modelValue"
        :error="error"
        :disabled="disabled"
      />
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1 text-title"
        :class="{
          'border-invalid text-invalid': error,
          'border-valid text-valid': valid,
          'border-disabled text-disabled bg-disabled': disabled,
        }"
      >
        <InputRadioIcon :checked="modelValue === option.value" class="mr-1" />
        <template v-if="option.label">
          {{ option.label }}
        </template>
        <template v-else>
          {{ option.value }}
        </template>
      </InputLabel>
    </div>

    <button
      v-show="isClearBtnShow"
      class="ml-2 w-8 text-center text-button-outline hover:text-button-outline hover:underline"
      :class="{ 'ml-2': align === 'horizontal', 'mt-2': align === 'vertical' }"
      type="reset"
      :id="`${id}-radio-group-clear`"
      :form="`${id}-radio-group`"
      @click.prevent="resetModelValue"
    >
      Clear
    </button>
  </div>
</template>

<script lang="ts" setup>
import {
  type InputProps,
  InputPropsDefaults,
  type IValueLabel,
} from "~/types/types";
import type { columnValue } from "metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    InputProps & {
      options: IValueLabel[];
      showClearButton?: boolean;
      align?: "horizontal" | "vertical";
    }
  >(),
  {
    ...InputPropsDefaults,
    showClearButton: false,
    align: "vertical",
  }
);
const modelValue = defineModel<columnValue>();
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
  modelValue.value = undefined;
}

const isClearBtnShow = computed(() => {
  return (
    props.showClearButton &&
    (modelValue.value === true ||
      modelValue.value === false ||
      (modelValue.value && modelValue.value !== ""))
  );
});
</script>
