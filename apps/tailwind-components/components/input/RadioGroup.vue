<template>
  <div
    :id="`${id}-radio-group`"
    :aria-describedby="describedBy"
    class="flex gap-1"
    :class="{
      'flex-row': align === 'horizontal',
      'flex-col': align === 'vertical',
    }"
  >
    <div v-for="option in options" class="flex justify-start align-center">
      <InputRadio
        :id="`${id}-radio-group-${option.value}`"
        class="opacity-0 absolute mt-1"
        :value="option.value"
        v-model="modelValue"
        @input="toggleSelect"
        :checked="option.value === modelValue"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
      />
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1 text-title"
      >
        <InputRadioIcon
          :checked="modelValue === option.value"
          class="mr-1"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
        />
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
import { type IInputProps, type IValueLabel } from "~/types/types";
import type { columnValue } from "metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      options: IValueLabel[];
      showClearButton?: boolean;
      align?: "horizontal" | "vertical";
    }
  >(),
  {
    align: "vertical",
  }
);
const modelValue = defineModel<columnValue>();
const emit = defineEmits([
  "update:modelValue",
  "select",
  "deselect",
  "blur",
  "focus",
]);

function toggleSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.checked) {
    emit("select", target.value);
  } else {
    emit("deselect", target.value);
  }
  emit("focus");
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
