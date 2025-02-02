<template>
  <InputGroup
    :id="id"
    :label="label"
    :errorMessage="errorMessage"
    :description="description"
    :required="required"
  >
    <component
      v-model="modelValue"
      v-if="inputComponent"
      :is="inputComponent"
      v-bind="$props"
      :description="description"
      @blur="$emit('blur')"
      @focus="$emit('focus')"
    />
  </InputGroup>
</template>

<script lang="ts" setup>
import {
  type InputProps,
  InputPropsDefaults,
  type IValueLabel,
} from "~/types/types";
import type { CellValueType } from "metadata-utils/src/types";
import {
  InputBoolean,
  InputCheckboxGroup,
  InputListbox,
  InputRadioGroup,
  InputRef,
  InputString,
  InputTextArea,
  InputPlaceHolder,
} from "#components";
const modelValue = defineModel<any>();
const props = withDefaults(
  defineProps<
    InputProps & {
      type: CellValueType;
      label?: string;
      description?: string | null;
      required?: boolean;
      errorMessage?: string | null;
      refSchemaId?: string;
      refTableId?: string;
      refLabel?: string;
      options?: IValueLabel[];
    }
  >(),
  {
    ...InputPropsDefaults,
    required: false,
  }
);

function typeToInputMap(type: CellValueType) {
  switch (type) {
    case "AUTO_ID":
    case "STRING":
    case "LONG":
      return InputString;
    case "CHECKBOX":
      return InputCheckboxGroup;
    case "RADIO":
      return InputRadioGroup;
    case "SELECT":
      return InputListbox;
    case "TEXT":
      return InputTextArea;
    case "REF":
      return InputRef;
    case "BOOL":
      return InputBoolean;
    default:
      return InputPlaceHolder;
  }
}

const inputComponent = computed(() => {
  return typeToInputMap(props.type.toUpperCase());
});
</script>
