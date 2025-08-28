<template>
  <vue-date-picker
    :id="id"
    type="date"
    :aria-describedby="describedBy"
    :placeholder="placeholder"
    :disabled="disabled"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
    :enable-time-picker="false"
    month-name-format="long"
    :text-input="{
      enterSubmit: true,
      tabSubmit: true,
      selectOnFocus: true,
      escClose: true,
      format: 'yyyy-MM-dd',
    }"
    format="yyyy-MM-dd"
  />
</template>

<script setup lang="ts">
import VueDatePicker from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import type { IInputProps } from "../../types/types";

defineProps<
  IInputProps & {
    type?: string;
  }
>();

const modelValue = defineModel<any>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
</script>
