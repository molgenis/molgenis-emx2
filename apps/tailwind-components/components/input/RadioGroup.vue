<template>
<<<<<<< HEAD
  <div :id="`${id}-radio-group`">
    <div
      class="flex justify-start align-center"
      v-for="option in options"
      :key="option.value"
    >
=======
  <div
    :id="`${id}-radio-group`"
    class="flex gap-1"
    :class="{
      'flex-row': align === 'horizontal',
      'flex-col': align === 'vertical',
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
import type { IValueLabel } from "~/types/types";
import type {columnValue} from "metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    id: string;
    options: IValueLabel[];
    showClearButton?: boolean;
    align?: "horizontal" | "vertical";
  }>(),
  {
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
