<template>
  <div :id="`${id}-radio-group`">
    <div
      class="flex justify-start align-center"
      v-for="option in options"
      :key="option.value"
    >
      <InputRadio
        :id="`${id}-radio-group-${option.value}`"
        class="sr-only"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        @input="toggleSelect"
        :checked="option.value === modelValue"
      />
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1"
      >
        <InputRadioIcon :checked="modelValue === option.value" class="mr-2.5" />
        <template v-if="option.label">
          {{ option.label }}
        </template>
        <template v-else>
          {{ option.value }}
        </template>
      </InputLabel>
    </div>
    <div class="mt-2" v-if="showClearButton">
      <button
        type="reset"
        :id="`${id}-radio-group-clear`"
        :form="`${id}-radio-group`"
        @click.prevent="resetModelValue"
      >
        Clear
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { IValueLabel } from "~/types/types";

withDefaults(
  defineProps<{
    id: string;
    options: IValueLabel[];
    showClearButton?: boolean;
  }>(),
  {
    showClearButton: false,
  }
);
const modelValue = defineModel<string>();
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
</script>
