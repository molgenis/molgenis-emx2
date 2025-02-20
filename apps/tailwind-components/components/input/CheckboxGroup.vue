<template>
  <div
    :id="`${id}-checkbox-group`"
    :aria-describedby="describedBy"
    class="border-l-4 border-transparent"
    :class="{
      'border-l-invalid': invalid,
      'border-l-valid': valid,
    }"
  >
    <div class="flex flex-row" v-for="option in options">
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="group flex justify-start items-center"
        :class="{
          'text-disabled cursor-not-allowed': disabled,
          'text-title cursor-pointer ': !disabled,
        }"
      >
        <input
          type="checkbox"
          :id="`${id}-checkbox-group-${option.value}`"
          :name="id"
          :value="option.value"
          v-model="modelValue"
          :checked="modelValue!.includes(option.value)"
          :disabled="disabled"
          @change="toggleSelect"
          class="opacity-0 absolute ml-4 mt-2"
        />
        <!-- don't use ssr only that make screen 'hop'. Also we want aria to be able to use it. It is just hidden behind the icon
        todo remove this comment once stable.
        -->
        <InputCheckboxIcon
          :checked="modelValue!.includes(option.value)"
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
  </div>
</template>

<script lang="ts" setup>
import { type IInputProps, type IValueLabel } from "~/types/types";
import type { columnValue } from "../../../metadata-utils/src/types";

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

const modelValue = defineModel<columnValue[]>();
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
  modelValue.value = [];
}
</script>
