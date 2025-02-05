<template>
  <InputRadioGroup
    :id="id"
    v-model="modelValue"
    :aria-describedby="describedBy"
    :options="yesNoOption"
    :showClearButton="true"
    align="horizontal"
    :state="state"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script lang="ts" setup>
import type { IInputProps, IRadioOptionsData } from "~/types/types";
const props = withDefaults(
  defineProps<
    IInputProps & {
      trueLabel?: string;
      falseLabel?: string;
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
