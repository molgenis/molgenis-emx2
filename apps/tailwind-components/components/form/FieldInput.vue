<script setup lang="ts">
import type {
  InputString,
  InputTextArea,
  InputPlaceHolder,
  InputListbox,
} from "#build/components";
import type {
  columnId,
  columnValue,
  CellValueType,
} from "../../../metadata-utils/src/types";

import type { IListboxValue } from "~/types/listbox";

type inputComponent =
  | InstanceType<typeof InputString>
  | InstanceType<typeof InputTextArea>
  | InstanceType<typeof InputPlaceHolder>;

defineProps<{
  type: CellValueType;
  id: columnId;
  label: string;
  required: boolean;
  data: columnValue;
  options?: IListboxValue[];
}>();

defineEmits(["focus", "error", "update:modelValue"]);
defineExpose({ validate });

const input = ref<inputComponent>();

function validate(value: columnValue) {
  if (!input.value) {
    throw new Error("FormFieldInput is not found in dom");
  }
  if (!input.value.validate) {
    throw new Error("FormFieldInput Component is missing validate method");
  }

  input.value.validate(value);
}
</script>

<template>
  <LazyInputString
    v-if="type === 'STRING'"
    ref="input"
    :id="id"
    :label="label"
    :required="required"
    :value="(data as string)"
    @focus="$emit('focus')"
    @update:modelValue="$emit('update:modelValue', $event)"
    @error="$emit('error', $event)"
  ></LazyInputString>
  <LazyInputTextArea
    v-else-if="type === 'TEXT'"
    ref="input"
    :id="id"
    :label="label"
    :required="required"
    :value="(data as string)"
    @focus="$emit('focus')"
    @update:modelValue="$emit('update:modelValue', $event)"
    @error="$emit('error', $event)"
  ></LazyInputTextArea>
  <template
    v-else-if="['ONTOLOGY', 'ONTOLOGY_ARRAY'].includes(type) && options"
  >
    <LazyInputListbox
      ref="input"
      :id="id"
      :label-id="id"
      :required="required"
      :options="options"
      :placeholder="`Select a ${id}`"
    />
  </template>
  <LazyInputPlaceHolder v-else ref="input" :type="type" />
</template>
