<template>
  <InputListbox
    :id="id"
    :options="values"
    :invalid="invalid"
    :valid="valid"
    :disabled="disabled"
    :placeholder="placeholder"
    :aria-describedby="describedBy"
    v-model="modelValue"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
</template>

<script lang="ts" setup>
import type { IInputProps } from "../../../types/types";
import type { IInputValue } from "../../../../metadata-utils/src/types";
import InputListbox from "./Listbox.vue";

withDefaults(
  defineProps<
    IInputProps & {
      values: string[];
    }
  >(),
  {
    placeholder: "Select an option",
  }
);

const modelValue = defineModel<IInputValue | null>();
const emit = defineEmits(["blur", "focus"]);
</script>
