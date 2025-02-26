<template>
  <div v-if="values" v-for="(value, index) in values" :key="index" class="flex">
    <Input
      :id="id"
      v-model="values[index]"
      v-bind="partialProps($props)"
      :type="nonArrayType(props.type || 'STRING_ARRAY')"
      @blur="emit('blur')"
      @focus="emit('focus')"
    />
    <Button iconOnly icon="trash" label="Remove"  v-if="values.length > 1"
      @click="clearInput(values, index)" />
    <Button iconOnly icon="plus" label="add" @click="addItem(values, index)" />
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "~/types/types";
import type { CellValueType } from "../../../metadata-utils/src/types";
const values = defineModel<string[]>();

let props = defineProps<
  IInputProps & {
    type?: string;
  }
>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function addItem(values: any, index: number) {
  values.splice(index + 1, 0, null);
  emit("update:modelValue", values);
}

function clearInput(values: any, index: number) {
  if (values.length > 1) {
    values.splice(index, 1);
  }
  emit("update:modelValue", values);
}

function partialProps(props: any): { [key: string]: string } {
  let clone = { ...props };
  delete clone.type;
  delete clone.modelValue;
  return clone;
}

function nonArrayType(type: string): CellValueType {
  return ({
    BOOL_ARRAY: "BOOL",
    DATE_ARRAY: "DATE",
    DATETIME_ARRAY: "DATETIME",
    DECIMAL_ARRAY: "DECIMAL",
    PERIOD_ARRAY: "PERIOD",
    EMAIL_ARRAY: "EMAIL",
    HYPERLINK_ARRAY: "HYPERLINK",
    INT_ARRAY: "INT",
    LONG_ARRAY: "LONG",
    STRING_ARRAY: "STRING",
    TEXT_ARRAY: "TEXT",
    UUID_ARRAY: "UUID",
  }[type] || "STRING") as CellValueType;
}
</script>
