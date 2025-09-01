<template>
  {{ modelValue }}
  <vue-date-picker
    :uid="id"
    :placeholder="datePlaceholder"
    :aria-describedby="describedBy"
    :disabled="disabled"
    :data-valid="valid"
    :data-invalid="invalid"
    type="Date"
    v-model="modelValue"
    model-type="format"
    month-name-format="long"
    :format="inputDateFormat"
    :auto-apply="false"
    :time-picker-inline="true"
    :text-input="{
      enterSubmit: true,
      tabSubmit: true,
      selectOnFocus: true,
      escClose: true,
    }"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import VueDatePicker from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import type { IInputProps } from "../../types/types";
import { watch, ref } from "vue";

const props = defineProps<
  IInputProps & {
    type?: string;
  }
>();

const inputDateFormat: string = "yyyy-MM-dd HH:mm";
const datePlaceholder = ref<string>(inputDateFormat);
const modelValue = defineModel<Date | string | null | undefined>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function setPlaceholder() {
  if (props.placeholder) {
    datePlaceholder.value = props.placeholder;
  } else {
    datePlaceholder.value = inputDateFormat;
  }
}

setPlaceholder();
watch(
  () => props.placeholder,
  () => setPlaceholder
);
</script>
