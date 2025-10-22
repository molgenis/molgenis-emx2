<template>
  <InputRadioGroup
    :id="id"
    v-model="modelValue"
    :aria-describedby="describedBy"
    :options="yesNoOption"
    :showClearButton="true"
    :align="align"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { IInputProps, IRadioOptionsData } from "../../../types/types";
import InputRadioGroup from "./RadioGroup.vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      trueLabel?: string;
      falseLabel?: string;
      align?: "horizontal" | "vertical";
    }
  >(),
  {
    trueLabel: "True",
    falseLabel: "False",
  }
);
const modelValue = defineModel<true | false | null>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

const yesNoOption = ref<IRadioOptionsData[]>([
  { value: true, label: props.trueLabel },
  { value: false, label: props.falseLabel },
]);
</script>
