<template>
  <div>
    <div
      v-for="(_value, index) in values"
      :key="index"
      class="flex items-center gap-1 mb-1"
    >
      <Input
        :id="id + '_' + index"
        :modelValue="values[index]"
        v-bind="getPartialProps($props)"
        :type="getNonArrayType(props.type || 'STRING_ARRAY')"
        @blur="emit('blur')"
        @focus="emit('focus')"
        @update:model-value="setValues($event as columnValue, index)"
      />
      <Button
        iconOnly
        type="secondary"
        icon="trash"
        label="Remove item"
        v-if="values.length > 1"
        @click="clearInput(values, index)"
      />
    </div>
    <Button
      class="my-[10px]"
      type="text"
      size="small"
      icon="plus"
      label="Add an additional item"
      @click="addItem(values)"
    />
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "../../../types/types";
import type {
  CellValueType,
  columnValue,
} from "../../../../metadata-utils/src/types";
import { ref } from "vue";
import Input from "../Input.vue";
import Button from "../Button.vue";

const props = defineProps<
  IInputProps & {
    modelValue: columnValue[] | undefined | null;
    type: string;
  }
>();

const values = ref<columnValue[]>(handleUndefined(props.modelValue));
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function handleUndefined(
  inputValues: columnValue[] | undefined | null
): columnValue[] {
  if (
    inputValues === undefined ||
    inputValues === null ||
    (Array.isArray(inputValues) && inputValues.length === 0)
  ) {
    return [null];
  }
  return inputValues;
}

function setValues(value: columnValue, index: number) {
  values.value[index] = value;
  emit("update:modelValue", values.value);
}

function addItem(values: columnValue[]) {
  values.push(null);
  emit("update:modelValue", values);
}

function clearInput(values: columnValue[], index: number) {
  if (values.length > 1) {
    values.splice(index, 1);
  }
  emit("update:modelValue", values);
}

function getPartialProps(props: any): { [key: string]: string } {
  let clone = { ...props };
  delete clone.id;
  delete clone.type;
  delete clone.modelValue;
  return clone;
}

function getNonArrayType(type: string): CellValueType {
  return ({
    BOOL_ARRAY: "BOOL",
    DATE_ARRAY: "DATE",
    DATETIME_ARRAY: "DATETIME",
    DECIMAL_ARRAY: "DECIMAL",
    PERIOD_ARRAY: "PERIOD",
    EMAIL_ARRAY: "EMAIL",
    HYPERLINK_ARRAY: "HYPERLINK",
    INT_ARRAY: "INT",
    NON_NEGATIVE_INT_ARRAY: "NON_NEGATIVE_INT",
    LONG_ARRAY: "LONG",
    STRING_ARRAY: "STRING",
    TEXT_ARRAY: "TEXT",
    UUID_ARRAY: "UUID",
  }[type] || "STRING") as CellValueType;
}
</script>
