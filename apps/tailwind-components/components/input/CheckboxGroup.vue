<template>
  <div :id="`${id}-checkbox-group`" :aria-describedby="describedBy">
    <div class="flex flex-row" v-for="option in options">
      <input
        type="checkbox"
        :id="`${id}-checkbox-group-${option.value}`"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        :checked="modelValue!.includes(option.value)"
        :disabled="disabled"
        @change="toggleSelect"
        @focus="$emit('focus')"
        class="sr-only fixed"
      />
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="hover:cursor-pointer flex justify-start items-center text-title"
      >
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
import { type IInputProps, type IValueLabel } from "~/types/types";

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

const modelValue = defineModel<string[]>();
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
}

function resetModelValue() {
  modelValue.value = [];
}
</script>
