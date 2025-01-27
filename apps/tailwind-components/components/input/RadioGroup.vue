<template>
  <div
    :id="`${id}-radio-group`"
    class="flex gap-1"
    :class="{
      'flex-row': align === 'horizontal',
      'flex-col': align === 'vertical',
    }"
  >
    <div v-for="option in radioOptions" class="flex justify-start align-center">
      <InputRadio
        :id="`${id}-radio-group-${option.value}`"
        class="sr-only fixed"
        :name="id"
        :value="option.value"
        :modelValue="props.modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        :checked="option.value === props.modelValue"
      />
      <InputLabel
        :for="`${id}-radio-group-${option.value}`"
        class="hover:cursor-pointer flex flex-row gap-1 text-title"
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
      v-show="
        showClearButton &&
        (modelValue === true ||
          modelValue === false ||
          (typeof modelValue === 'string' && modelValue.length > 0))
      "
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
import type { RadioOptionsDataIF } from "~/types/types";

const props = withDefaults(
  defineProps<{
    id: string;
    modelValue: boolean | string | null;
    radioOptions: RadioOptionsDataIF[];
    showClearButton?: boolean;
    align?: "horizontal" | "vertical";
  }>(),
  {
    showClearButton: false,
    align: "vertical",
  }
);

const emit = defineEmits(["update:modelValue"]);

function resetModelValue() {
  emit("update:modelValue", null);
}
</script>
