<template>
  <client-only>
    <vue-date-picker
      :uid="id"
      :placeholder="datePlaceholder"
      :aria-describedby="describedBy"
      :disabled="disabled"
      :data-valid="valid"
      :data-invalid="invalid"
      type="Date"
      v-model:="internalValue"
      @update:modelValue="handleUpdate"
      model-type="format"
      month-name-format="long"
      :format="inputDateFormat"
      :auto-apply="true"
      :time-picker-inline="true"
      :enable-seconds="true"
      :text-input="{
        enterSubmit: true,
        tabSubmit: true,
        selectOnFocus: true,
        escClose: true,
      }"
      @blur="handleBlur"
    />
  </client-only>
</template>

<script setup lang="ts">
import VueDatePicker from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";
import type { IInputProps } from "../../../types/types";
import type { DateValue } from "../../../../metadata-utils/src/types";
import { watch, ref, onMounted } from "vue";

const props = defineProps<
  IInputProps & {
    modelValue: DateValue;
  }
>();

const inputDateFormat: string = "yyyy-MM-dd HH:mm:ss";
const datePlaceholder = ref<string>(inputDateFormat);
//vue-date-picker emitted to many events so we need a filter
const internalValue = ref<DateValue>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function setPlaceholder(value?: DateValue) {
  if (value) {
    datePlaceholder.value = value as string;
  } else {
    datePlaceholder.value = inputDateFormat;
  }
}

function handleUpdate(newValue: string) {
  if (newValue !== props.modelValue) {
    emit("update:modelValue", newValue);
  }
}

function handleBlur() {
  //check that actually something relevant happened
  if (internalValue.value !== props.modelValue) {
    emit("blur");
  }
}

onMounted(() => {
  internalValue.value = props.modelValue;
  setPlaceholder(props.modelValue);
});

watch(
  () => props.placeholder,
  (value) => {
    setPlaceholder(value);
  }
);

watch(
  () => props.modelValue,
  () => {
    internalValue.value = props.modelValue;
  }
);
</script>
