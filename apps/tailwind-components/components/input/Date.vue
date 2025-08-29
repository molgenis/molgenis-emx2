<template>
  {{ modelValue }}
  <vue-date-picker
    :uid="id"
    :placeholder="placeholder"
    :aria-describedby="describedBy"
    :disabled="disabled"
    :data-valid="valid"
    :data-invalid="invalid"
    type="Date"
    v-model="modelValue"
    model-type="format"
    month-name-format="long"
    format="yyyy-MM-dd"
    :auto-apply="true"
    :enable-time-picker="false"
    :text-input="{
      enterSubmit: true,
      tabSubmit: true,
      selectOnFocus: true,
      escClose: true,
      // format: 'yyyy-MM-dd',
    }"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
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
