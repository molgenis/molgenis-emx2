<script lang="ts" setup>
import { type IInputProps, type IValueLabel } from "../../../types/types";
import type { columnValue } from "../../../../metadata-utils/src/types";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import InputLabel from "../input/Label.vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import ButtonText from "../button/Text.vue";

withDefaults(
  defineProps<
    IInputProps & {
      options: IValueLabel[];
      showClearButton?: boolean;
    }
  >(),
  {
    showClearButton: false,
  }
);

const modelValue = defineModel<columnValue[] | undefined | null>();
const emit = defineEmits([
  "update:modelValue",
  "select",
  "deselect",
  "blur",
  "focus",
]);

function isChecked(value: columnValue): boolean {
  return Array.isArray(modelValue.value) && modelValue.value.includes(value);
}

function toggleSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const currentArray = Array.isArray(modelValue.value) ? modelValue.value : [];

  if (target.checked) {
    modelValue.value = [...currentArray, target.value];
    emit("select", target.value);
  } else {
    modelValue.value = currentArray.filter((v) => v !== target.value);
    emit("deselect", target.value);
  }
  emit("focus");
}

function resetModelValue() {
  modelValue.value = [];
}
</script>

<template>
  <InputGroupContainer
    :id="`${id}-checkbox-group`"
    :aria-describedby="describedBy"
    @focus="emit('focus')"
    @blur="emit('blur')"
  >
    <div class="flex flex-row" v-for="option in options">
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="group flex justify-start items-center relative"
        :class="{
          'text-disabled cursor-not-allowed': disabled,
          'text-title-contrast cursor-pointer ': !disabled,
        }"
      >
        <input
          type="checkbox"
          :id="`${id}-checkbox-group-${option.value}`"
          :name="id"
          :value="option.value"
          :checked="isChecked(option.value)"
          :disabled="disabled"
          @change="toggleSelect"
          class="ml-4 mt-2 sr-only"
        />
        <InputCheckboxIcon
          :checked="isChecked(option.value)"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
        />
        <span class="block" v-if="option.label">
          {{ option.label }}
        </span>
        <span class="block" v-else>
          {{ option.value }}
        </span>
      </InputLabel>
    </div>
    <ButtonText
      v-if="showClearButton"
      type="reset"
      :id="`${id}-checkbox-group-clear`"
      class="mt-2 ml-3"
      :form="`${id}-checkbox-group`"
      @click.prevent="resetModelValue"
      :disabled="disabled || null"
    >
      Clear
    </ButtonText>
  </InputGroupContainer>
</template>
