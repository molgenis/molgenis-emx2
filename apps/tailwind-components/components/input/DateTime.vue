<template>
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
    :auto-apply="true"
    :time-picker-inline="true"
    :enable-seconds="true"
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
import type { DateValue } from "../../../metadata-utils/src/types";
import { watch, ref, onBeforeMount } from "vue";

const props = defineProps<
  IInputProps & {
    modelValue: DateValue;
  }
>();

const inputDateFormat: string = "yyyy-MM-dd HH:mm:ss";
const datePlaceholder = ref<string>(inputDateFormat);
const modelValue = defineModel<DateValue>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function setPlaceholder(value?: DateValue) {
  if (value) {
    datePlaceholder.value = value as string;
  } else {
    datePlaceholder.value = inputDateFormat;
  }
}

function setModelValue(value: DateValue) {
  modelValue.value = value ? (value as string).split("T").join(" ") : value;
}

onBeforeMount(() => {
  setModelValue(props.modelValue);
  setPlaceholder(props.modelValue);
});

watch(
  () => props.placeholder,
  (value) => {
    setPlaceholder(value);
  }
);

watch(
  () => modelValue.value,
  () => {
    emit("update:modelValue", modelValue.value);
  }
);
</script>
