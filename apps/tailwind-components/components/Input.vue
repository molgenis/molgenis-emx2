<template>
  <InputGroup
    :id="id"
    :label="label"
    :errorMessage="errorMessage"
    :description="description"
  >
    <component
      v-model="modelValue"
      v-if="inputComponent"
      :is="inputComponent"
      v-bind="$props"
      :error="error || !!errorMessage"
      :description="description"
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

const typeToInputMap: Record<CellValueType, any> = {
  // AUTO_ID: InputString,
  //HEADING: InputHeading,
  // EMAIL: InputEmail,
  // HYPERLINK: InputHyperlink,
  STRING: InputString,
  CHECKBOX: InputCheckboxGroup,
  RADIO: InputRadioGroup,
  SELECT: InputListbox,
  TEXT: InputTextArea,
  REF: InputRef,
  // JSON: InputJson,
  // INT: InputInt,
  // LONG: InputLong,
  // DECIMAL: InputDecimal,
  BOOL: InputBoolean,
  // DATE: InputDate,
  // REF: InputRefSelect,
  // REFBACK: InputRefBack,
  // FILE: InputFile,
  // DATETIME: InputDateTime,
  // PERIOD: InputString,
  // ONTOLOGY: InputOntology,
  // EMAIL_ARRAY: ArrayInput,
  // BOOL_ARRAY: ArrayInput,
  // DATE_ARRAY: ArrayInput,
  // DATETIME_ARRAY: ArrayInput,
  // PERIOD_ARRAY: ArrayInput,
  // DECIMAL_ARRAY: ArrayInput,
  // HYPERLINK_ARRAY: ArrayInput,
  // INT_ARRAY: ArrayInput,
  // LONG_ARRAY: ArrayInput,
  // ONTOLOGY_ARRAY: InputOntology,
  // REF_ARRAY: InputRefList,
  // STRING_ARRAY: ArrayInput,
  // TEXT_ARRAY: ArrayInput,
};

const inputComponent = computed(() => {
  return typeToInputMap[props.type.toUpperCase()];
});
</script>
