<template>
  <div
    :id="`${id}-radio-group`"
    :aria-describedby="describedBy"
    class="flex gap-1 border-l-4 border-transparent"
    :class="{
      'flex-row': align === 'horizontal',
      'flex-col': align === 'vertical',
      'border-l-invalid': invalid,
      'border-l-valid': valid,
    }"
  >
    <div v-for="option in options" class="flex flex-row">
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="group flex justify-start items-center gap-1"
        :class="{
          'text-disabled cursor-not-allowed': disabled,
          'text-title cursor-pointer ': !disabled,
        }"
      >
        <InputRadio
          :id="`${id}-radio-group-${option.value}`"
          class="peer sr-only"
          :value="option.value"
          :name="id"
          v-model="modelValue"
          @input="toggleSelect"
          :checked="option.value === modelValue"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
        />
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
    <ButtonText
      v-if="showClearButton"
      type="reset"
      :id="`${id}-radio-group-clear`"
      class="w-8 ml-3"
      :form="`${id}-radio-group`"
      @click.prevent="resetModelValue"
      :disabled="disabled || null"
    >
      Clear
    </ButtonText>
  </div>
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../metadata-utils/src/types";
import type { IInputProps, IValueLabel } from "~/types/types";

withDefaults(
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
</script>
