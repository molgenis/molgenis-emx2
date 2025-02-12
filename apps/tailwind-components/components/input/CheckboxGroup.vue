<template>
  <div :id="`${id}-checkbox-group`" :aria-describedby="describedBy">
    <div class="group flex flex-row" v-for="option in options">
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="group cursor-pointer flex justify-start items-center text-title"
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
          class="peer sr-only"
        />
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
    <div class="mt-2" v-if="showClearButton">
      <ButtonText
        type="reset"
        :id="`${id}-checkbox-group-clear`"
        :form="`${id}-checkbox-group`"
        @click.prevent="resetModelValue"
      >
        Clear
      </ButtonText>
    </div>
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
