<template>
  <InputRadioGroup
      v-bind="$props"
      :options="options"
    :showClearButton="true"
    v-model="modelValue"
    align="horizontal"
  />
</template>

<script lang="ts" setup>
import type {InputProps, IRadioOptionsData} from "~/types/types";
import {InputPropsDefaults} from "~/types/types";
const props = withDefaults(
  defineProps<InputProps &{
    trueLabel?: string;
    falseLabel?: string;
  }>(),
  {
    ...InputPropsDefaults,
    trueLabel: "True",
    falseLabel: "False",
  }
);
const modelValue = defineModel<true | false | null>();
const emit = defineEmits([
  "focus",
  "blur",
  "update:modelValue",
]);

const options = ref<IRadioOptionsData[]>([
  { value: true, label: props.trueLabel },
  { value: false, label: props.falseLabel },
]);
</script>
