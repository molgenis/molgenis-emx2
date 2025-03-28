<template>
  <div>
    <div
      v-for="(_value, index) in values"
      :key="index"
      class="flex items-center gap-1"
    >
      <Input
        :id="id + '_' + index"
        :modelValue="values[index]"
        v-bind="partialProps($props)"
        :type="nonArrayType(props.type || 'STRING_ARRAY')"
        @blur="emit('blur')"
        @focus="emit('focus')"
        @update:model-value="setValues($event, index)"
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
      type="secondary"
      size="small"
      icon="plus"
      label="Add a additional item"
      @click="addItem(values)"
    />
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "~/types/types";
import type { CellValueType } from "../../../metadata-utils/src/types";

let props = defineProps<
  IInputProps & {
    modelValue: any[] | undefined;
    type?: string;
  }
>();

const values = ref<any[]>(handleUndefined(props.modelValue));
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function handleUndefined(bla: any[] | undefined) {
  if (bla === undefined) {
    return [undefined];
  }
  return bla;
}

function setValues(value: any, index: number) {
  values.value[index] = value;
  emit("update:modelValue", values.value);
}

function addItem(values: any) {
  values.push(undefined);
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
  delete clone.id;
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
