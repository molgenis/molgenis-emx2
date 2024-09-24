<script setup lang="ts">
import type { InputString, InputTextArea } from "#build/components";
import type {
  columnId,
  columnValue,
  CellValueType,
} from "../../../metadata-utils/src/types";

type inputComponent =
  | InstanceType<typeof InputString>
  | InstanceType<typeof InputTextArea>;

defineProps<{
  type: CellValueType;
  id: columnId;
  label: string;
  data: columnValue;
}>();

defineEmits(["focus", "input", "error", "update:modelValue"]);
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
    :value="data as string"
    @focus="$emit('focus')"
    @input="$emit('input')"
    @update:modelValue="$emit('update:modelValue', $event)"
    @error="$emit('error', $event)"
  ></LazyInputString>
  <LazyInputTextArea
    v-else-if="type === 'TEXT'"
    ref="input"
    :id="id"
    @focus="$emit('focus')"
    @input="$emit('input')"
  ></LazyInputTextArea>
  <div v-else class="border border-dotted p-2">
    <pre>place holder for field type {{ type }}</pre>
  </div>
</template>
