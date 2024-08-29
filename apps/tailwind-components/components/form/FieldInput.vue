<script setup lang="ts">
import type {
  columnId,
  columnValue,
  ValueType,
} from "../../../metadata-utils/src/types";

defineProps<{
  type: ValueType;
  id: columnId;
  data: columnValue;
}>();

defineEmits(["focus", "input"]);
</script>

<template>
  <LazyInputString
    v-if="type === 'STRING'"
    :id="id"
    @focus="$emit('focus')"
    @input="$emit('input')"
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
