<script setup lang="ts">
import type {
  columnId,
  columnValue,
  ValueType,
} from "../../../metadata-utils/src/types";

defineProps<{
  type: ValueType;
  id: columnId;
  label: string;
  data: columnValue;
}>();

defineEmits(["focus", "input", "error", "update:modelValue"]);
</script>

<template>
  <LazyInputString
    v-if="type === 'STRING'"
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
    :id="id"
    @focus="$emit('focus')"
    @input="$emit('input')"
  ></LazyInputTextArea>
  <div v-else class="border border-dotted p-2">
    <pre>place holder for field type {{ type }}</pre>
  </div>
</template>
